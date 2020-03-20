package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.similarity.TreeDistanceMetric;

import java.util.Collection;
import java.util.List;

/**
 * This policy will add individuals to the pool if the pool does not contain something phenotypically similar.
 * In case a similar item exists, the policy updates the fitness of the item to the average fitness and increment its duplicate
 * count but does not add it again. This can increase the pool size indefinitely.
 */
public class UnboundedPhenotypicUpdatePolicy implements KNNPoolUpdatePolicy
{
    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source,
                                          PoolFilter filter, TreeDistanceMetric metric, List<ReactiveDecisionSituation> dps, Object ... extra)
    {
//        PhenotypicTreeSimilarityMetric metric = (PhenotypicTreeSimilarityMetric)extra[0];
        if(filter == null)
            filter = new ExpFeasibleNoRefillPoolFilter();
        for(Individual ind : inds)
        {
            if(ind.fitness.fitness() == Double.POSITIVE_INFINITY || ind.fitness.fitness() == Double.NEGATIVE_INFINITY)
                continue;

            GPRoutingPolicy indPolicy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
            double minDis = Double.POSITIVE_INFINITY;
            KNNPoolItem best = null;
            for(KNNPoolItem p : pool)
            {
                double dist = metric.distance(indPolicy, p.policy);
                if (dist < minDis)
                {
                    minDis = dist;
                    best = p;
                }
                if (dist == 0)
                    break;
            }
            if(best == null || minDis != 0)
                pool.add(new KNNPoolItem((GPIndividual)ind, filter, source));
            else // found an exact match
            {
                double oldFitness = best.fitness;
                best.fitness = (oldFitness * (best.nDuplicates + 1) + ind.fitness.fitness() ) / (++best.nDuplicates + 1);
            }
        }

        return pool;
    }

    @Override
    public String getName()
    {
        return "UnboundedPhenotypic";
    }
}
