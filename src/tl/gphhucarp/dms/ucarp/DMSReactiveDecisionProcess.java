package tl.gphhucarp.dms.ucarp;

import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;

import java.util.PriorityQueue;

public class DMSReactiveDecisionProcess extends DMSDecisionProcess {

	public DMSReactiveDecisionProcess(DecisionProcessState state,
								   PriorityQueue<DecisionProcessEvent> eventQueue,
								   RoutingPolicy routingPolicy)
	{
		super(state, eventQueue, routingPolicy, null);
	}

	@Override
	public void reset() {
		state.reset();
		eventQueue.clear();
		for (NodeSeqRoute route : state.getSolution().getRoutes())
			eventQueue.add(new ReactiveRefillEvent(0, route));
	}

//    @Override
//    protected ReactiveDecisionProcess clone()
//    {
//        // TODO: Replace with copy constructor
//        DecisionProcessState clonedState = new DecisionProcessState(state); // .clone();
//        PriorityQueue<DecisionProcessEvent> clonedEQ = new PriorityQueue<>(eventQueue);
//
//        return new ReactiveDecisionProcess(clonedState, clonedEQ, routingPolicy);
//    }
}
