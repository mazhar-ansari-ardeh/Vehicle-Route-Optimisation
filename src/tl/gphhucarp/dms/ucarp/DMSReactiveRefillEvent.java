package tl.gphhucarp.dms.ucarp;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.reactive.event.ReactiveServingEvent;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;

import java.util.LinkedList;
import java.util.List;

public class DMSReactiveRefillEvent extends DMSDecisionProcessEvent {

	private NodeSeqRoute route;

	public DMSReactiveRefillEvent(double time, NodeSeqRoute route)
	{
		super(time);
		this.route = route;
	}

	@Override
	public void trigger(DecisionProcess decisionProcess)
	{
		RoutingPolicy policy = decisionProcess.getRoutingPolicy();
		DecisionProcessState state = decisionProcess.getState();
		Instance instance = state.getInstance();
		Graph graph = instance.getGraph();
		int depot = instance.getDepot();

		int currNode = route.currNode();

		if (currNode == depot) {
			// refill when arriving the depot
			route.setDemand(0);

			if (state.getUnassignedTasks().isEmpty()) {
				// if there is no unassigned tasks, then no need to go out again.
				// stay at the depot and close the route
				return;
			}

			// calculate the route-to-task map
			state.calcRouteToTaskMap(route);

			// decide which task to serve next
			List<Arc> pool = new LinkedList<>(state.getUnassignedTasks());

			ReactiveDecisionSituation rds = new ReactiveDecisionSituation(
					pool, route, state);
			recordDecisionSituation(rds);

			Arc nextTask = policy.next(rds);

			state.removeUnassignedTasks(nextTask);
			route.setNextTask(nextTask);

			decisionProcess.getEventQueue().add(
					new ReactiveServingEvent(route.getCost(), route, nextTask));
		}
		else {
			// continue going to the depot if not arrived yet
			int nextNode = graph.getPathTo(currNode, depot);

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
				graph.recalcEstDistanceBetween(currNode, depot);
				nextNode = graph.getPathTo(currNode, depot);
			}

			// add the traverse to the next node
			route.add(nextNode, 0, instance);
			// add a new event
			decisionProcess.getEventQueue().add(
					new gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent(route.getCost(), route));
		}
	}
}
