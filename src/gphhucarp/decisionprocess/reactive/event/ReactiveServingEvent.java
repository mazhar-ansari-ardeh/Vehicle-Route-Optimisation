package gphhucarp.decisionprocess.reactive.event;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The reactive serving event occurs when the vehicle is on the way
 * to serve the next task. Its target node is the head node of the next task.
 * The event is triggered whenever the vehicle arrives a node along the path.
 */

public class ReactiveServingEvent extends DecisionProcessEvent {
    private NodeSeqRoute route;
    private Arc nextTask;

    public ReactiveServingEvent(double time,
                                NodeSeqRoute route, Arc nextTask) {
        super(time);
        this.route = route;
        this.nextTask = nextTask;
    }

    @Override
    public void trigger(DecisionProcess decisionProcess) {
        RoutingPolicy policy = decisionProcess.getRoutingPolicy();
        DecisionProcessState state = decisionProcess.getState();
        Instance instance = state.getInstance();
        Graph graph = instance.getGraph();
        int depot = instance.getDepot();

        int currNode = route.currNode();

        // refill the capacity if the current node is the depot
        if (currNode == depot)
            route.setDemand(0);

        if (currNode == nextTask.getFrom()) {
            // start serving the next task if it arrives its head node
            double remainingCapacity = instance.getCapacity() - route.getDemand();
            double nextTaskRemainingFrac = state.getTaskRemainingDemandFrac(nextTask);
            double remainingDemand = instance.getActDemand(nextTask) * nextTaskRemainingFrac;

            if (remainingDemand > remainingCapacity) {
                // a route failure occurs, refill and then come back
                double servedFraction = remainingCapacity / remainingDemand;

                // add the partial service to the route
                route.add(nextTask.getTo(), servedFraction, instance);
                // update the remaining demand fraction of the task
                state.setTaskRemainingDemandFrac(nextTask, nextTaskRemainingFrac - servedFraction);
                // add a new event: go to the depot to refill, and come back to
                // continue the failed service.
                decisionProcess.getEventQueue().add(
                        new ReactiveRefillThenServeEvent(route.getCost(), route, nextTask));
            }
            else {
                // no route failure occurs, can complete the service successfully
                route.add(nextTask.getTo(), nextTaskRemainingFrac, instance);
                // update the remaining demand fraction of the task
                state.setTaskRemainingDemandFrac(nextTask, 0.0);
                // remove the task from the remaining tasks
                state.removeRemainingTasks(nextTask);
                // update the task-to-task and route-to-task maps
                state.completeTask(nextTask);
                // add a new serving event.
                decisionProcess.getEventQueue().add(
                        new ReactiveServingEvent(route.getCost(), route, nextTask));
            }
        }
        else if (currNode == nextTask.getTo() &&
                Double.compare(state.getTaskRemainingDemandFrac(nextTask), 0.0) == 0) {
            // calculate the route-to-task map
            state.calcRouteToTaskMap(route);

            // decide which task to serve next
            List<Arc> pool = new LinkedList<>(state.getUnassignedTasks());

            ReactiveDecisionSituation rds = new ReactiveDecisionSituation(
                    pool, route, state);

            nextTask = policy.next(rds);

            if (nextTask == null) {
                // go back to the depot to refill, if the depot loop is selected
                route.setNextTask(instance.getDepotLoop());

                decisionProcess.getEventQueue().add(
                        new ReactiveRefillEvent(route.getCost(), route));
            }
            else {
                state.removeUnassignedTasks(nextTask);
                route.setNextTask(nextTask);
                decisionProcess.getEventQueue().add(
                        new ReactiveServingEvent(route.getCost(), route, nextTask));
            }
        }
        else {
            // go to the next node if has not arrived the target node yet
            int nextNode = graph.getPathTo(currNode, nextTask.getFrom());

            // check the accessibility of all the arcs going out from the arrived node.
            boolean edgeFailure = false; // edge failure: next node is not accessible.
            for (Arc arc : graph.getOutNeighbour(currNode)) {
                if (instance.getActDeadheadingCost(arc) == Double.POSITIVE_INFINITY) {
                    graph.updateEstCostMatrix(arc.getFrom(), arc.getTo(), Double.POSITIVE_INFINITY);

                    if (arc.getTo() == nextNode)
                        edgeFailure = true;
                }
            }

            // recalculate the shortest path based on the new cost matrix.
            // update the next node in the new shortest path.
            if (edgeFailure) {
                graph.recalcEstDistanceBetween(currNode, nextTask.getFrom());
                nextNode = graph.getPathTo(currNode, nextTask.getFrom());
            }

            // add the traverse to the next node
            route.add(nextNode, 0, instance);
            // add a new event
            decisionProcess.getEventQueue().add(
                    new ReactiveServingEvent(route.getCost(), route, nextTask));
        }
    }

}
