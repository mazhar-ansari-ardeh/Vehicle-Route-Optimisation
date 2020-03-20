package tl.knowledge.surrogate.knn;

import ec.Individual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.similarity.TreeDistanceMetric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

class KNNPool implements Iterable<KNNPoolItem>
{
    Collection<KNNPoolItem> pool = new ArrayList<>();

    void setUpdatePolicy(KNNPoolUpdatePolicy updatePolicy)
    {
        this.updatePolicy = updatePolicy;
    }

    private KNNPoolUpdatePolicy updatePolicy = new FIFONoDupPhenotypicUpdatePolicy(1024);

    @Override
    public Iterator<KNNPoolItem> iterator()
    {
        return pool.iterator();
    }

//    void update(GPIndividual ind)
//    {
//
//    }

    public void reevaluate(Function<Individual, Double> with)
    {
        if(pool == null)
            return;

        for(KNNPoolItem item : pool)
        {
            Individual ind = item.policy.getGPTree().owner;
            item.fitness = with.apply(ind);
            // Reset the number of duplicates. The importance of this field is that it can help us learn the test fitness
            // gradually.
            item.nDuplicates = 0;
        }
    }

    public void update(Individual[] inds, String source, PoolFilter filter, TreeDistanceMetric metric, List<ReactiveDecisionSituation> dps, Object... extra)
    {
        pool = updatePolicy.update(pool, inds, source, filter, metric, dps, extra);
    }

    String poolLog()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(KNNPoolItem.CSVHeader());
        pool.forEach(i -> sb.append(i.toCSVString()));

        return sb.toString();
    }
}


