package gphhucarp.decisionprocess;

import ec.gp.GPNode;
import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.UCARPPrimitiveSet;
import gphhucarp.representation.route.NodeSeqRoute;
import gputils.DoubleData;

import java.util.HashMap;
import java.util.List;

/**
 * A routing policy makes a decision
 */

public abstract class RoutingPolicy implements Cloneable {

    protected String name;
    protected PoolFilter poolFilter;
    protected TieBreaker tieBreaker;

    public RoutingPolicy(PoolFilter poolFilter, TieBreaker tieBreaker) {
        this.poolFilter = poolFilter;
        this.tieBreaker = tieBreaker;
    }

    public RoutingPolicy(PoolFilter poolFilter) {
        this(poolFilter, SimpleTieBreaker.getInstance());
    }

    public RoutingPolicy(TieBreaker tieBreaker) {
        this(new IdentityPoolFilter(), tieBreaker);
    }

    public RoutingPolicy(RoutingPolicy other)
    {
        if(other == null)
            throw new NullPointerException("Cannot clone a null object.");

        this.name = other.name;
        this.poolFilter = (PoolFilter) other.poolFilter.clone();
        this.tieBreaker = (TieBreaker) other.tieBreaker.clone();
    }

    @Override
    public Object clone()
    {
        try
        {
            RoutingPolicy retval = (RoutingPolicy) super.clone();
            retval.tieBreaker = (TieBreaker) tieBreaker.clone();
            retval.poolFilter = (PoolFilter) poolFilter.clone();
            retval.name = name;
            return retval;
        } catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public PoolFilter getPoolFilter() {
        return poolFilter;
    }

    public TieBreaker getTieBreaker() {
        return tieBreaker;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Given the current decison process state,
     * select the next task to serve by the give route from the pool of tasks.
     * @param rds the reactive decision situation.
     * @return the next task to be served by the route.
     */
    public Arc next(ReactiveDecisionSituation rds) {
        List<Arc> pool = rds.getPool();
        NodeSeqRoute route = rds.getRoute();
        DecisionProcessState state = rds.getState();

        List<Arc> filteredPool = poolFilter.filter(pool, route, state);

        if (filteredPool.isEmpty())
            return null;

        Arc next = filteredPool.get(0);
        next.setPriority(priority(next, route, state));
//        boolean log = false;
//        if(log)
//            System.out.println(next.toString() + ": " + next.getPriority());

        for (int i = 1; i < filteredPool.size(); i++) {
            Arc tmp = filteredPool.get(i);
            tmp.setPriority(priority(tmp, route, state));
//            if(log)
//                System.out.println(tmp.toString() + ": " + tmp.getPriority());

            if (Double.compare(tmp.getPriority(), next.getPriority()) < 0 ||
                    (Double.compare(tmp.getPriority(), next.getPriority()) == 0 &&
                            tieBreaker.breakTie(tmp, next) < 0))
                next = tmp;
        }

        return next;
    }

    public void setPoolFilter(PoolFilter poolFilter)
    {
        this.poolFilter = poolFilter;
    }

    //    HashMap<String, Double> terminalValues(Arc candidate, NodeSeqRoute route, DecisionProcessState state, ReactiveDecisionSituation rds)
//    {
//        HashMap<String, Double> retval = new HashMap<>();
//        List<GPNode> terminals = UCARPPrimitiveSet.wholeTerminalSet().getList();
//        CalcPriorityProblem calcPrioProb = new CalcPriorityProblem(candidate, route, state);
//        DoubleData data = new DoubleData();
//        for(GPNode terminal : terminals)
//        {
//            terminal.eval(null, 0, data, null, null, calcPrioProb);
//            retval.put(terminal.toString(), data.value);
////            System.out.println(terminal.toString() + ": " + data.value);
//        }
////        System.out.println();
////        state.
//        return retval;
//    }



    /**
     * Given the current decision process state,
     * whether to continue the service of the planned task or not.
     * @param plannedTask the planned task to be served next.
     * @param route the current route.
     * @param state the decision process state.
     * @return true if continue to serve the planned task, and false otherwise.
     */
    public boolean continueService(Arc plannedTask, NodeSeqRoute route, DecisionProcessState state) {
        if (priority(plannedTask, route, state) < 0)
            return false;

        return true;
    }

    /**
     * Calculate the priority of a candidate task for a route given a state.
     * @param candidate the candidate task.
     * @param route the route.
     * @param state the state.
     * @return the priority of the candidate task.
     */
    public abstract double priority(Arc candidate,
                                    NodeSeqRoute route,
                                    DecisionProcessState state);
}
