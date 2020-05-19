package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gp.similarity.TreeSimilarityMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This policy will initialise a pool with the given items in the first call and then does not update it at all. Basically,
 * this class implements a fixed and invariant KNN pool.
 */
public class AddOncePhenotypicUpdatePolicy implements KNNPoolUpdatePolicy
{
    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
										  TreeSimilarityMetric metric, List<ReactiveDecisionSituation> dps, Object... extra)
    {
        if(pool != null && !pool.isEmpty())
            return pool;

        if(filter == null)
            filter = new ExpFeasibleNoRefillPoolFilter();

        Arrays.sort(inds);
        List<Individual> sortedInds = Arrays.asList(inds);
        if(metric instanceof SituationBasedTreeSimilarityMetric)
        {
            SimpleNichingAlgorithm.clearPopulation(sortedInds, (SituationBasedTreeSimilarityMetric) metric, 0, 1);
        }
        else
            SimpleNichingAlgorithm.clearPopulation(sortedInds, dps, 0, 1);

        final ArrayList<KNNPoolItem> retval = new ArrayList<>();
        for (Individual i : sortedInds)
        {
            if (i.fitness.fitness() != Double.POSITIVE_INFINITY)
                retval.add(new KNNPoolItem((GPIndividual) i, filter, source));
        }

        return retval;
    }

    @Override
    public String getName()
    {
        return "AddOncePhenotypic";
    }
}
