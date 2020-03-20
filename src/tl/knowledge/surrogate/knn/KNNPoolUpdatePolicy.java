package tl.knowledge.surrogate.knn;

import ec.Individual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.similarity.TreeDistanceMetric;

import java.util.Collection;
import java.util.List;

public interface KNNPoolUpdatePolicy
{
    Collection<KNNPoolItem> update(Collection<KNNPoolItem> pool, Individual[] inds, String source, PoolFilter filter,
                                   TreeDistanceMetric metric, List<ReactiveDecisionSituation> dps, Object... extra);

    String getName();
}
