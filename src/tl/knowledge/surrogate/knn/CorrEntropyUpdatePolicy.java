package tl.knowledge.surrogate.knn;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.TreeSimilarityMetric;
import tl.knowledge.surrogate.knn.dbscan.Cluster;
import tl.knowledge.surrogate.knn.dbscan.Clusterable;
import tl.knowledge.surrogate.knn.dbscan.DBScanClusterer;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class CorrEntropyUpdatePolicy implements KNNPoolUpdatePolicy, TLLogger
{
    private final int maxPoolSize;
    private final File logFolder;

    private final boolean updateFitnessDuplicates;

    private EvolutionState state;

    private int logID;

    private int updateCount = 0;

    private double eps;

    private int minClusterSize;

    /**
     *
     * @param poolSize
     * @param state
     * @param logFolder
     * @param updateFitnessDuplicates if {@code true}, the fitness of new items that have existing duplicates in the
     *                                pool will be set to the average of the duplicates. Otherwise, just the fitness
     *                                of the new individual will be used.
     */
    public CorrEntropyUpdatePolicy(int poolSize, EvolutionState state, File logFolder, boolean updateFitnessDuplicates,
                                   double eps, int minClusterSize)
    {
        this.maxPoolSize = poolSize;
        this.state = state;
        this.logFolder = logFolder;
        this.updateFitnessDuplicates = updateFitnessDuplicates;
        this.eps = eps;
        this.minClusterSize = minClusterSize;

        logID = setupLogger(state, new File(logFolder, "clusterLog.0.txt").getAbsolutePath());
    }

    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source,
                                          PoolFilter filter, TreeSimilarityMetric metric,
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

            if(updateFitnessDuplicates)
                fitness = (fitness + ind.fitness.fitness() ) / (totalDup + 1);
            else
                fitness = ind.fitness.fitness();
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

        DBScanClusterer<KNNItemClusterable> clusterer = new DBScanClusterer<>(eps, minClusterSize);
        List<KNNItemClusterable> collect = pool.stream().
                map(item -> new KNNItemClusterable(item, (CorrPhenoTreeSimilarityMetric) metric)).collect(Collectors.toList());
        List<Cluster<KNNItemClusterable>> clusters = clusterer.cluster(collect);

        double entropy = entropy(clusterer, pool.size());
        logClusters("Pool before update: " + entropy, clusters);
        pool.forEach(i -> log(state, logID, i.toCSVString(), "\n"));
        log(state, logID, "\n");

        for (int i = 0; i < newItems.size(); i++) {
            KNNPoolItem newItem = newItems.get(i);
            update((ArrayList<KNNPoolItem>) pool, clusterer, newItem, (CorrPhenoTreeSimilarityMetric) metric);
        }

        clusters = clusterer.getClusters();
        entropy = entropy(clusterer, pool.size());
        logClusters("Pool after update: " + entropy, clusters);
        pool.forEach(i -> log(state, logID, i.toCSVString(), "\n"));

        updateCount++;
        closeLogger(state, logID);
        logID = setupLogger(state, new File(logFolder, "clusterLog." + updateCount + ".txt").getAbsolutePath());

        return pool;
    }

    private static double entropy(DBScanClusterer<KNNItemClusterable> clusterer, int poolSize)
    {
        double entropy = 0;
        List<Cluster<KNNItemClusterable>> clusters = clusterer.getClusters();
        ArrayList<KNNItemClusterable> noises = clusterer.getNoises();

        for(Cluster<KNNItemClusterable> cluster : clusters)
        {
            int clusterSize = cluster.getPoints().size();
            if(clusterSize == 0) // for any reason
                continue;
            double p = clusterSize / ((double)poolSize);
            entropy += p * Math.log(p);
        }

        for(int i = 0; i < noises.size(); i++)
        {
            double p = 1 / ((double)poolSize);
            entropy += p * Math.log(p);
        }

        return -entropy;
    }

//    private static double entropy(List<Cluster<KNNItemClusterable>> clusters, int poolSize)
//    {
//        if(clusters == null)
//            return 0;
//
//        double entropy = 0;
//        for(Cluster<KNNItemClusterable> cluster : clusters)
//        {
//            int clusterSize = cluster.getPoints().size();
//            if(clusterSize == 0) // for any reason
//                continue;
//            double p = clusterSize / ((double)poolSize);
//            entropy += p * Math.log(p);
//        }
//
//        return -entropy;
//    }

    private void update(ArrayList<KNNPoolItem> pool, DBScanClusterer<KNNItemClusterable> clusterer, KNNPoolItem newItem,
                        CorrPhenoTreeSimilarityMetric dist)
    {
        int poolSize = pool.size();

//        List<Cluster<KNNItemClusterable>> clusters = clusterer.getClusters();

        double maxEntropy = entropy(clusterer, poolSize);
        KNNItemClusterable kItem = new KNNItemClusterable(newItem, dist);
        clusterer.addPoint(kItem);

        KNNPoolItem toRemove = null;
        for (int i = 0; i < pool.size(); i++) {
            KNNPoolItem p = pool.get(i);
            KNNItemClusterable feature = new KNNItemClusterable(p, dist);
            Cluster<KNNItemClusterable> remFrom = clusterer.removePoint(feature);
            double newEntropy = entropy(clusterer, poolSize);
            if (remFrom == null)
                clusterer.addPoint(feature);
            else
                clusterer.addPoint(remFrom, feature);

            if (newEntropy > maxEntropy) {
                maxEntropy = newEntropy;
                toRemove = p;
            }
        }
        if (toRemove != null)
        {
            pool.remove(toRemove);
            pool.add(newItem);

            KNNItemClusterable feaToRemove = new KNNItemClusterable(toRemove, dist);

            clusterer.removePoint(feaToRemove);
//            clusterer.addPoint(kItem);


            log(state, logID, feaToRemove.toString() + " removed.\n");
            log(state, logID, kItem.toString() + " added.\n");
        }
        else
        {
            clusterer.removePoint(kItem);
            log(state, logID, "Item discarded: " + newItem.fitness + "\n");
        }
    }

    private void logClusters(String message, List<Cluster<KNNItemClusterable>> clusters)
    {
        log(state, logID, message + "\n");
        clusters.forEach(value -> {
            log(state, logID, "-------------------------- CLUSTER ----------------------------\n");

            // Sorting the coordinates to see the most significant tags first.
//            log(state, logID, key.toString() + "\n");
            String members = value.getPoints().stream().sorted().map(Object::toString).collect(Collectors.joining("\n"));
            log(state, logID, members + "\n");

            log(state, logID, "\n");
            log(state, logID, "\n");
        });
    }

    @Override
    public String getName()
    {
        return "CorrEntropy";
    }

    static class KNNItemClusterable implements Clusterable<KNNItemClusterable>, Comparable<KNNItemClusterable> {
        KNNItemClusterable(KNNPoolItem item, CorrPhenoTreeSimilarityMetric dist)
        {
            this.item = item;
            this.dist = dist;
        }

        private KNNPoolItem item;
        private CorrPhenoTreeSimilarityMetric dist;

        @Override
        public double distanceFrom(KNNItemClusterable p)
        {
            return dist.distance(item.policy, p.item.policy);
        }

        @Override
        public KNNItemClusterable centroidOf(Collection<KNNItemClusterable> p)
        {
            return null;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KNNItemClusterable that = (KNNItemClusterable) o;

            return that.item.policy.getGPTree().treeEquals(item.policy.getGPTree());
        }

        @Override
        public String toString() {
            return item.toCSVString();
        }

        @Override
        public int hashCode()
        {
            // TODO: Is this a good idea? Maybe it's better to hashCode the phenotypic characterisation
            return item.policy.getGPTree().treeHashCode();
//            return dist.characterise(this.item.policy).hashCode();
        }

        @Override
        public int compareTo(KNNItemClusterable o) {
            return Double.compare(item.fitness, o.item.fitness);
        }
    }
}
