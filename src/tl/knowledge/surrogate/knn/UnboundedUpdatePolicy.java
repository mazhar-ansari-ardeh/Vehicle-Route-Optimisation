package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.similarity.TreeSimilarityMetric;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class UnboundedUpdatePolicy implements KNNPoolUpdatePolicy
{

    @Override
    public Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
										  TreeSimilarityMetric metric, List<ReactiveDecisionSituation> dps, Object ... extra)
    {
        Arrays.stream(inds).map(i -> new KNNPoolItem(((GPIndividual)i), filter, source)).forEach(pool::add);
        return pool;
    }

    @Override
    public String getName()
    {
        return "Unbounded";
    }
}
