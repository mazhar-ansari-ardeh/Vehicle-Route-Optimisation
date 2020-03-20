package tl.knowledge.surrogate.knn;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.MersenneTwisterFast;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.TreeDistanceMetric;
import tl.knowledge.surrogate.knn.kmeans.Centroid;
import tl.knowledge.surrogate.knn.kmeans.EuclideanDistance;
import tl.knowledge.surrogate.knn.kmeans.KMeans;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class EntropyUpdatePolicy implements KNNPoolUpdatePolicy, TLLogger
{
    private final int maxPoolSize;
    private final File logFolder;

    private MersenneTwisterFast rand;

    /**
     * The number of clusters of k-means.
     */
    private int k;

    private static final EuclideanDistance euDist = new EuclideanDistance();

    private EvolutionState state;

    private int logID;

    private int updateCount = 0;

    public EntropyUpdatePolicy(int poolSize, EvolutionState state, int threadnum, int k, File logFolder)
    {
        this.maxPoolSize = poolSize;
        this.rand = state.random[threadnum];
        this.state = state;
        this.k = k;
        this.logFolder = logFolder;

        logID = setupLogger(state, new File(logFolder, "clusterLog.0.txt").getAbsolutePath());
    }

    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter, TreeDistanceMetric metric,
                                          List<ReactiveDecisionSituation> dps, Object... extra)
    {
        if(filter == null)
            filter = new ExpFeasibleNoRefillPoolFilter();

        Arrays.sort(inds);
        List<Individual> sortedInds = Arrays.asList(inds);
        SimpleNichingAlgorithm.clearPopulation(sortedInds, dps, 0, 1);

        ArrayList<KNNPoolItem> newItems = new ArrayList<>();
        for(Individual ind : sortedInds)
        {
            if(ind.fitness.fitness() == Double.POSITIVE_INFINITY)
                continue;

            ArrayList<KNNPoolItem> duplicates = new ArrayList<>();
            for (KNNPoolItem knnPoolItem : pool)
            {
                if (metric.distance(new GPRoutingPolicy(filter, ((GPIndividual) ind).trees[0]), knnPoolItem.policy) == 0)
                {
                    duplicates.add(knnPoolItem);
                }
            }

            double fitness = 0;
            int totalDup = 0;
            for (KNNPoolItem dup : duplicates)
            {
                totalDup += (dup.nDuplicates + 1);
                fitness += dup.fitness * (dup.nDuplicates + 1);
            }

            pool.removeAll(duplicates);
            KNNPoolItem item = new KNNPoolItem((GPIndividual) ind, filter, source);
            item.nDuplicates = totalDup;

            fitness = (fitness + ind.fitness.fitness() ) / (totalDup + 1);
            item.fitness = fitness;

            if(pool.size() < maxPoolSize)
                pool.add(item);
            else
            {
                newItems.add(item);
            }
        }
        if(!(pool instanceof ArrayList))
            pool = new ArrayList<>(pool);

        Map<Centroid, List<FeatureVector>> cl = kmeans(pool, k, metric, rand);
        double entropy = entropy(cl, pool.size());
        logClusters("Pool before update: " + entropy, cl);
        pool.forEach(i -> log(state, logID, i.toCSVString(), "\n"));
        log(state, logID, "\n");

        for (KNNPoolItem newItem : newItems)
        {
            update((ArrayList<KNNPoolItem>) pool, newItem, rand, metric);
        }

        entropy = entropy(cl, pool.size());
        logClusters("Pool after update: " + entropy, cl);
        pool.forEach(i -> log(state, logID, i.toCSVString(), "\n"));

        updateCount++;
        closeLogger(state, logID);
        logID = setupLogger(state, new File(logFolder, "clusterLog." + updateCount + ".txt").getAbsolutePath());

        return pool;
    }

    private static Map<Centroid, List<FeatureVector>> kmeans(Collection<KNNPoolItem> pool, int k, TreeDistanceMetric dist
            , MersenneTwisterFast rand)
    {
        assert !pool.isEmpty();

        List<FeatureVector> collect =
                pool.stream().map(item -> new KNNItemFeature(item, dist)).collect(Collectors.toList());

//        for(Centroid c : cl.keySet())
//        {
//            System.out.println(c.toString());
//            List<FeatureVector> l = cl.get(c);
//            System.out.println(l.toString() + "\n");
//        }

        return KMeans.fit(collect, k, new EuclideanDistance(), 10000, rand);
    }

    private void update(ArrayList<KNNPoolItem> pool, Map<Centroid, List<FeatureVector>> cl, KNNPoolItem item, TreeDistanceMetric dist)
    {
        int poolSize = pool.size();
        double maxEntropy = entropy(cl, poolSize);

        KNNItemFeature kItem = new KNNItemFeature(item, dist);
        Centroid centroid = KMeans.nearestCentroid(kItem, cl.keySet(), euDist);
        cl.get(centroid).add(kItem);

        KNNPoolItem toRemove = null;
        for(KNNPoolItem p : pool)
        {
            KNNItemFeature feature = new KNNItemFeature(p, dist);
            centroid = KMeans.nearestCentroid(feature, cl.keySet(), euDist);
            cl.get(centroid).remove(feature);
            double newEntropy = entropy(cl, poolSize);
            cl.get(centroid).add(feature);
            if(newEntropy > maxEntropy)
            {
                maxEntropy = newEntropy;
                toRemove = p;
            }
        }
        if(toRemove != null)
        {
            pool.remove(toRemove);
            pool.add(item);

            KNNItemFeature feaToRemove = new KNNItemFeature(toRemove, dist);

            cl.get(centroid).remove(feaToRemove);
            Centroid receiver = KMeans.nearestCentroid(kItem, cl.keySet(), euDist);
            cl.get(receiver).add(kItem);


            log(state, logID, feaToRemove.toString() + " removed from " + centroid.toString() + "\n");
            log(state, logID, kItem.toString() + " added to " + receiver.toString() + "\n");
        }
        else
        {
            log(state, logID, "Item discarded: " + item.fitness + "\n");
        }
    }
    private void update(ArrayList<KNNPoolItem> pool, KNNPoolItem item, MersenneTwisterFast rand, TreeDistanceMetric dist)
    {
        Map<Centroid, List<FeatureVector>> cl = kmeans(pool, k, dist, rand);

        update(pool, cl, item, dist);
    }

    private static double entropy(Map<Centroid, List<FeatureVector>> clusters, int poolSize)
    {
        if(clusters == null)
            return 0;

        double entropy = 0;
        for(Centroid center : clusters.keySet())
        {
            List<FeatureVector> cluster = clusters.get(center);
            if(cluster.size() == 0) // for any reason
                continue;
            double p = cluster.size() / ((double)poolSize);
            entropy += p * Math.log(p);
        }

        return -entropy;
    }

    private void logClusters(String message, Map<Centroid, List<FeatureVector>> clusters)
    {
        log(state, logID, message + "\n");
        clusters.forEach((key, value) -> {
            log(state, logID, "-------------------------- CLUSTER ----------------------------\n");

            // Sorting the coordinates to see the most significant tags first.
            log(state, logID, key.toString() + "\n");
            String members = value.stream().sorted().map(Object::toString).collect(Collectors.joining("\n"));
            log(state, logID, members + "\n");

            log(state, logID, "\n");
            log(state, logID, "\n");
        });
    }

    @Override
    public String getName()
    {
        return "Entropy";
    }

    static class KNNItemFeature implements FeatureVector, Comparable<KNNItemFeature>
    {
        KNNItemFeature(KNNPoolItem item, TreeDistanceMetric<int[]> dist)
        {
            this.item = item;
            this.dist = dist;
        }

        private KNNPoolItem item;
        private TreeDistanceMetric<int[]> dist;

        @Override
        public double[] getFeatures()
        {
            int[] ch = dist.characterise(item.policy);
            double[] features = new double[ch.length];
            for (int i = 0; i < features.length; i++)
            {
                features[i] = (double) ch[i];
            }
            return features;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            KNNItemFeature centroid = (KNNItemFeature) o;
            return Arrays.equals(getFeatures(), centroid.getFeatures());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(getFeatures());
        }

        @Override
        public String toString()
        {
            double[] a = getFeatures();
            if (a == null)
                return "null";
            int iMax = a.length - 1;
            if (iMax == -1)
                return "[]";

            StringBuilder b = new StringBuilder();
            for (int i = 0; ; i++) {
                b.append(a[i]);
                if (i == iMax)
                    break;
                b.append(", ");
            }
            return item.fitness + ", " + b.toString();
        }

        @Override
        public int compareTo(KNNItemFeature o)
        {
            return Double.compare(item.fitness, o.item.fitness);
        }
    }


//    private static double entropy(List<KNNPoolItem> pool, MersenneTwisterFast rand)
//    {
//        Map<Centroid, List<FeatureVector>> cl = KMeans.fit(pool, 10, new EuclideanDistance(), 10000, rand);
//
//        return entropy(cl, pool.size());
//    }

//    private static double entropy(ArrayList<List<KNNPoolItem>> partitions, int poolSize)
//    {
////        ArrayList<List<KNNPoolItem>> partitions = partition(pool, dist);
//
//        double entropy = 0;
//
//        for(List<KNNPoolItem> part : partitions)
//        {
//            if(part.size() == 0) // for any reason
//                continue;
//            double p = part.size() / ((double)poolSize);
//            entropy += p * Math.log(p);
//        }
//
//        return -entropy;
//    }
//
//    private static void updatePartition(ArrayList<List<KNNPoolItem>> partitions, KNNPoolItem add,
//                                        KNNPoolItem remove, BiFunction<KNNPoolItem, KNNPoolItem, Double> dist)
//    {
//        List<KNNPoolItem> partRem = null;
//        for (int i = 0; remove != null && i < partitions.size(); i++)
//        {
//            List<KNNPoolItem> part = partitions.get(i);
//
//            if (part.remove(remove))
//            {
//                partRem = part;
//                break;
//            }
//        }
//        if(partRem != null)
//        {
//            if (partRem.isEmpty())
//                partitions.remove(partRem);
//        }
////        else
////            System.out.println("Whay is it empty?");
//
//        boolean added = add == null; // if add is null, ignore adding.
//        for (int i = 0; !added && i < partitions.size(); i++)
//        {
//            List<KNNPoolItem> part = partitions.get(i);
//            for (KNNPoolItem item : part)
//            {
//                if (dist.apply(item, add) < 1)
//                {
//                    part.add(add);
//                    added = true;
//                    break;
//                }
//            }
//        }
//        if(!added)
//        {
//            ArrayList<KNNPoolItem> newPart = new ArrayList<>();
//            newPart.add(add);
//            partitions.add(newPart);
//        }
//    }
}
