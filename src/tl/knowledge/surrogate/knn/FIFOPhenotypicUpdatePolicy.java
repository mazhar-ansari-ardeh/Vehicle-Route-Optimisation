package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.TreeSimilarityMetric;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements the first-in, last out policy with duplicates. This means that the policy first clears the given
 * pool of individuals. Then, it adds the individual to the pool and if the pool size exceeds the max pool size, it removes
 * the oldest one from the pool. The idea to remove the oldest one is motivated by the observation that usually, there is a
 * good co-relation between the newer individuals.
 */
public class FIFOPhenotypicUpdatePolicy implements KNNPoolUpdatePolicy
{

    private int poolMaxSize;

    public FIFOPhenotypicUpdatePolicy(int poolMaxSize)
    {
//        this.dps = dps;
        this.poolMaxSize = poolMaxSize;
    }

    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
										  TreeSimilarityMetric metric, List<ReactiveDecisionSituation> dps, Object... extra)
    {
        final LinkedList<KNNPoolItem> retval; // = new LinkedList<>();
        if(pool == null)
        {
            retval = new LinkedList<>();
        }
        else if (!(pool instanceof LinkedList))
            retval = new LinkedList<>(pool);
        else
            retval = (LinkedList<KNNPoolItem>) pool;

        if(filter == null)
            filter = new ExpFeasibleNoRefillPoolFilter();

        Arrays.sort(inds);
        List<Individual> sortedInds = Arrays.asList(inds);
        SimpleNichingAlgorithm.clearPopulation(sortedInds, dps, 0, 1);

        for (Individual i : sortedInds)
        {
            if (i.fitness.fitness() == Double.POSITIVE_INFINITY)
                continue;
            retval.addFirst(new KNNPoolItem((GPIndividual) i, filter, source));
            if (retval.size() > poolMaxSize)
                retval.removeLast();
        }

        return retval;
    }

    @Override
    public String getName()
    {
        return "FIFOPhenotypic";
    }
}
