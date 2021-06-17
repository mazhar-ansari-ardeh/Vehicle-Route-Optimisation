package tl.gp.entropy;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.knowledge.surrogate.knn.CorrEntropyUpdatePolicy;
import tl.knowledge.surrogate.knn.dbscan.Cluster;
import tl.knowledge.surrogate.knn.dbscan.Clusterable;
import tl.knowledge.surrogate.knn.dbscan.DBScanClusterer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Entropy
{
    SituationBasedTreeSimilarityMetric metric;
    int nClusters;

    public int getnClusters() {
        return nClusters;
    }

    public int getnNoise() {
        return nNoise;
    }

    int nNoise;
    public Entropy(List<ReactiveDecisionSituation> situations)
    {
        metric = new HammingPhenoTreeSimilarityMetric();
        metric.setSituations(situations);
    }
    public double entropy(Individual[] inds, int tree, PoolFilter filter)
    {
        return entropy(inds, metric, tree, filter);
    }

    @Override
    public String toString()
    {
        if(clusterer == null)
            return "";

        StringBuilder retval = new StringBuilder();

        List<Cluster<GPIndsClusterable>> clusters = clusterer.getClusters();
        int cluster;
        for(cluster = 0; cluster < clusters.size(); cluster++)
        {
            retval.append("Cluster ").append(cluster).append(":\n");
            for(int j = 0; j < clusters.get(cluster).getPoints().size(); j++)
            {
                GPIndsClusterable gpIndsClusterable = clusters.get(cluster).getPoints().get(j);
                retval.append(gpIndsClusterable.toString()).append("\n");
            }
            retval.append("\n");
        }

        ArrayList<GPIndsClusterable> noises = clusterer.getNoises();
        for(int noise = 0; noise < noises.size(); noise++)
        {
            retval.append("Cluster ").append(cluster++).append(":\n");
            GPIndsClusterable gpIndsClusterable = noises.get(noise);
            retval.append(gpIndsClusterable.toString()).append("\n");
            retval.append("\n");
        }

        return retval.toString();
    }

    DBScanClusterer<GPIndsClusterable> clusterer;


    public double entropy(Individual[] inds, SituationBasedTreeSimilarityMetric metric, int tree, PoolFilter filter)
    {
        List<GPIndsClusterable> pool = Arrays.stream(inds).map(
                i -> new GPIndsClusterable(i, tree, filter, metric)).collect(Collectors.toList());
        clusterer = new DBScanClusterer<>(0, 1);
        List<Cluster<GPIndsClusterable>> clusters = clusterer.cluster(pool);

        double entropy = 0;
        ArrayList<GPIndsClusterable> noises = clusterer.getNoises();

        for(Cluster<GPIndsClusterable> cluster : clusters)
        {
            int clusterSize = cluster.getPoints().size();
            if(clusterSize == 0) // for any reason
                continue;
            double p = clusterSize / ((double)pool.size());
            entropy += p * Math.log(p);
        }

        for(int i = 0; i < noises.size(); i++)
        {
            double p = 1 / ((double)pool.size());
            entropy += p * Math.log(p);
        }
        nClusters = clusters.size();
        nNoise = noises.size();

        return -entropy;
    }
}

class GPIndsClusterable implements Clusterable<GPIndsClusterable>, Comparable<GPIndsClusterable>
{
    GPIndsClusterable(Individual item, int tree, PoolFilter filter, SituationBasedTreeSimilarityMetric dist)

    {
        if(!(item instanceof GPIndividual))
            throw new RuntimeException("Not a GP instance");

        this.item = new GPRoutingPolicy(filter, ((GPIndividual) item).trees[tree]);
        this.dist = dist;
    }

    private final GPRoutingPolicy item;
    private final SituationBasedTreeSimilarityMetric dist;

    @Override
    public double distanceFrom(GPIndsClusterable p)
    {
        return dist.distance(item, p.item);
    }

    @Override
    public GPIndsClusterable centroidOf(Collection<GPIndsClusterable> p)
    {
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GPIndsClusterable that = (GPIndsClusterable) o;

        return that.item.getGPTree().treeEquals(item.getGPTree());
    }

    @Override
    public int hashCode()
    {
        // TODO: Is this a good idea? Maybe it's better to hashCode the phenotypic characterisation
        return item.getGPTree().treeHashCode();
    }

    @Override
    public int compareTo(GPIndsClusterable o) {
        return Double.compare(item.getGPTree().owner.fitness.fitness(), o.item.getGPTree().owner.fitness.fitness());
    }

    @Override
    public String toString() {
        return item.getGPTree().owner.toString();
    }
}