package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.TreeSimilarityMetric;

import java.util.*;

/**
 * This class implements the first-in, first out policy with no duplicates. This means that the policy first clears the given
 * pool of individuals. Then, for each individual, it searches the pool and if the pool contains the individual, removes
 * the duplicates from the pool and updates the fitness value to the average. Finally, it adds the individual to the pool and
 * if the pool size exceeds the max pool size, it removes the oldest one from the pool. The idea to remove the oldest one is
 * motivated by the observation that usually, there is a good co-relation between the newer individuals.
 */
public class FIFONoDupPhenotypicUpdatePolicy implements KNNPoolUpdatePolicy
{

    private int poolMaxSize;

    public FIFONoDupPhenotypicUpdatePolicy(int poolMaxSize)
    {
        this.poolMaxSize = poolMaxSize;
    }

    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
										  TreeSimilarityMetric metric, List<ReactiveDecisionSituation> dps, Object... extra)
    {
        final LinkedList<KNNPoolItem> retval; // = new LinkedList<>();
        if(pool == null || pool.isEmpty())
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
            if(i.fitness.fitness() == Double.POSITIVE_INFINITY)
                continue;

            ArrayList<KNNPoolItem> duplicates = new ArrayList<>();
            for (KNNPoolItem knnPoolItem : retval)
            {
                if (metric.distance(new GPRoutingPolicy(filter, ((GPIndividual) i).trees[0]), knnPoolItem.policy) == 0)
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

            retval.removeAll(duplicates);
            KNNPoolItem I = new KNNPoolItem((GPIndividual) i, filter, source);
            I.nDuplicates = totalDup;

            fitness = (fitness + i.fitness.fitness() ) / (totalDup + 1);
            I.fitness = fitness;

            retval.addFirst(I);
            if (retval.size() > poolMaxSize) // This happens when no duplicate is found.
                retval.removeLast();
        }

        return retval;
    }

    @Override
    public String getName()
    {
        return "FIFONoDupPhenotypic";
    }
}
