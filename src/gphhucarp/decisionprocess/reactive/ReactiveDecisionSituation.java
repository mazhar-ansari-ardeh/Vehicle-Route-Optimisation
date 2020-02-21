package gphhucarp.decisionprocess.reactive;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.representation.route.NodeSeqRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * The decision situation for reactive decision process.
 */

public class ReactiveDecisionSituation extends DecisionSituation {

    private List<Arc> pool;
    private NodeSeqRoute route;
    private DecisionProcessState state;

    public ReactiveDecisionSituation(List<Arc> pool, NodeSeqRoute route, DecisionProcessState state) {
        // TODO: Should this constructor clone the received objects?
//        this.pool = new ArrayList<>(pool.size());
//        pool.forEach(item -> this.pool.add(Arc.cachedCopy(item)));
//        this.route = (NodeSeqRoute) route.clone();
//        this.state = new DecisionProcessState(state);

        this.pool = pool;
        this.route = route;
        this.state = state;
    }

    public ReactiveDecisionSituation(ReactiveDecisionSituation other)
    {
        if(other.pool != null)
        {
            this.pool = new ArrayList<>(other.pool.size());
            other.pool.forEach(arc -> this.pool.add(Arc.cachedCopy(arc)));
        }
        if(other.route != null)
            this.route = (NodeSeqRoute) other.route.clone();

        if(other.state != null)
            this.state = new DecisionProcessState(other.state);
    }

    public List<Arc> getPool() {
        return pool;
    }

    public NodeSeqRoute getRoute() {
        return route;
    }

    public DecisionProcessState getState() {
        return state;
    }

//    public ReactiveDecisionSituation clone() {
//        // TODO: Replace with cachedCopy constructor
//        List<Arc> clonedPool = new LinkedList<>(pool);
//        NodeSeqRoute clonedRoute = (NodeSeqRoute)route.clone();
//        DecisionProcessState clonedState = new DecisionProcessState(state); // .clone();
//
//        return new ReactiveDecisionSituation(clonedPool, clonedRoute, clonedState);
//    }
}
