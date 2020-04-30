package tl.gp.similarity;

import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.List;

public interface SituationBasedTreeSimilarityMetric extends TreeSimilarityMetric
{
	void setSituations(List<ReactiveDecisionSituation> situations);

	/**
	 * The name of this metric. Each class that implements this interface must have a unique name. The name helps to
	 * identify the metric in parameter files.
	 * @return The name of the metric.
	 */
	String getName();
}
