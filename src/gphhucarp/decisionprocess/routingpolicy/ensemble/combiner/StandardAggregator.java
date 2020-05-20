package gphhucarp.decisionprocess.routingpolicy.ensemble.combiner;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.ensemble.Combiner;
import gphhucarp.decisionprocess.routingpolicy.ensemble.EnsemblePolicy;
import gphhucarp.representation.route.NodeSeqRoute;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.ArrayList;
import java.util.List;

public class StandardAggregator extends Combiner
{
	@Override
	public Arc next(List<Arc> pool, NodeSeqRoute route, DecisionProcessState state, EnsemblePolicy ensemblePolicy) {
		Arc next = pool.get(0);
		next.setPriority(priority(next, route, state, ensemblePolicy));

		for (int i = 1; i < pool.size(); i++) {
			Arc tmp = pool.get(i);
			tmp.setPriority(priority(tmp, route, state, ensemblePolicy));

			if (Double.compare(tmp.getPriority(), next.getPriority()) < 0 ||
					(Double.compare(tmp.getPriority(), next.getPriority()) == 0 &&
							ensemblePolicy.getTieBreaker().breakTie(tmp, next) < 0))
				next = tmp;
		}

		return next;
	}

	public double priority(Arc arc, NodeSeqRoute route, DecisionProcessState state, EnsemblePolicy ensemblePolicy) {
		ArrayList<Double> priorities = new ArrayList<>();
		SummaryStatistics statistics = new SummaryStatistics();
		double priority = 0;
		for (int i = 0; i < ensemblePolicy.size(); i++) {
			priority = ensemblePolicy.getPolicy(i).priority(arc, route, state);
			statistics.addValue(priority);
			priorities.add(priority);
		}

		double min = statistics.getMin();
		double max = statistics.getMax();
		for(int i = 0; i < priorities.size(); i++)
		{
			double p = (priorities.get(i) - min) / (max - min);
			priorities.set(i, p);
		}

		return priority;
	}

	public void next(ReactiveDecisionSituation rds) {
		List<Arc> pool = rds.getPool();
		NodeSeqRoute route = rds.getRoute();
		DecisionProcessState state = rds.getState();
	}
}
