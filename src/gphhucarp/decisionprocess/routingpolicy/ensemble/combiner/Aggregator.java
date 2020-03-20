package gphhucarp.decisionprocess.routingpolicy.ensemble.combiner;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.ensemble.EnsemblePolicy;
import gphhucarp.decisionprocess.routingpolicy.ensemble.Combiner;
import gphhucarp.representation.route.NodeSeqRoute;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.*;

/**
 * The aggregator combiner simply sums up the weighted priority calculated by all the elements,
 * and set the final priority as the weighted sum.
 */

public class Aggregator extends Combiner {

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

    /**
     * Calculate the priority of a candidate arc by an ensemble policy.
     * @param arc the arc whose priority is to be calculated.
     * @param route the route.
     * @param state the decision process state.
     * @param ensemblePolicy the ensemble policy.
     * @return the priority of the arc calculated by the ensemble policy.
     */
    public double priority(Arc arc, NodeSeqRoute route, DecisionProcessState state, EnsemblePolicy ensemblePolicy) {
        double priority = 0;
        for (int i = 0; i < ensemblePolicy.size(); i++) {
            priority += ensemblePolicy.getPolicy(i).priority(arc, route, state) *
                    ensemblePolicy.getWeight(i);
        }

        return priority;
    }

    public double priority(Arc arc, NodeSeqRoute route, DecisionProcessState state, ArrayList<RoutingPolicy> policies) {
        double priority = 0;
        for (int i = 0; i < policies.size(); i++) {
            priority += policies.get(i).priority(arc, route, state);
        }

        return priority;
    }

    public List<Arc> priority(List<Arc> arcs, NodeSeqRoute route, DecisionProcessState state, ArrayList<GPRoutingPolicy> policies) {
        if(arcs == null)
            throw new IllegalArgumentException("List of arcs cannot be null.");
        if(route == null)
            throw new IllegalArgumentException("route cannot be null.");
        if(state == null)
            throw new IllegalArgumentException("state cannot be null.");
        if(policies == null)
            throw new IllegalArgumentException("policies cannot be null.");

        List<Double> aggregatedPriorities = new ArrayList<>(Collections.nCopies(arcs.size(), 0.0));
        for(RoutingPolicy policy : policies)
        {
            SummaryStatistics ss = new SummaryStatistics();
            for (int i = 0; i < arcs.size(); i++)
            {
                Arc arc = arcs.get(i);
                double priority = policy.priority(arc, route, state);
                arc.setPriority(priority);
                ss.addValue(priority);
            }
            double max = ss.getMax();
            double min = ss.getMin();
            for(int i = 0; i < arcs.size(); i++)
            {
                double aggPriority = aggregatedPriorities.get(i); //.getPriority();
                double priority = arcs.get(i).getPriority();
//                System.out.println(arcs.get(i).toString() + ":: " + priority);
                aggPriority += ((priority - min)/(max - min));
                aggregatedPriorities.set(i, aggPriority);
                arcs.get(i).setPriority(aggPriority);
            }
        }

        return arcs;
    }

    public List<Arc> priority(ReactiveDecisionSituation situation, ArrayList<GPRoutingPolicy> policies)
    {
        if(situation == null)
            throw new IllegalArgumentException("situation cannot be null.");
        if(policies == null)
            throw new IllegalArgumentException("policies cannot be null.");

        return priority(situation.getPool(), situation.getRoute(), situation.getState(), policies);
    }

    public Arc next(ReactiveDecisionSituation situation, ArrayList<GPRoutingPolicy> policies, TieBreaker tieBreaker)
    {
        if(situation == null)
            throw new IllegalArgumentException("situation cannot be null.");
        if(policies == null)
            throw new IllegalArgumentException("policies cannot be null.");

        List<Arc> priorities = priority(situation, policies);
        Arc next = null;
        for(Arc arc : priorities)
        {
            if(next == null)
            {
                next = arc;
                next.setPriority(arc.getPriority());
                continue;
            }
            double arcPriority = arc.getPriority(); // priorities.get(arc);
            if (arcPriority < next.getPriority() || (arcPriority == next.getPriority() && tieBreaker.breakTie(arc, next) < 0))
            {
                next = arc;
                next.setPriority(arcPriority);
            }
        }

        return next;
    }
}
