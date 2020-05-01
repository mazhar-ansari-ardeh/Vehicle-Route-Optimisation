package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.similarity.TreeSimilarityMetric;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This policy resets the pool every time that the pool is updated. That is, every time, it deletes the pool and adds the
 * new individuals into the pool. This policy does not perform any modifications on the given set of new individuals and just
 * adds them as they are.
 */
public class ResetUpdatePolicy implements KNNPoolUpdatePolicy
{
    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
										  TreeSimilarityMetric metric, List<ReactiveDecisionSituation> dps, Object ... extra)
    {
        return Arrays.stream(inds).map(i -> new KNNPoolItem((GPIndividual) i, filter, source)).collect(Collectors.toList());
    }

    @Override
    public String getName()
    {
        return "Reset";
    }
}
