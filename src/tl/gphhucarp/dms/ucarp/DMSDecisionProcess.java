package tl.gphhucarp.dms.ucarp;

import gphhucarp.core.Instance;
import gphhucarp.decisionprocess.*;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.decisionprocess.reactive.event.ReactiveRefillEvent;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;

import java.util.ArrayList;
import java.util.PriorityQueue;

public abstract class DMSDecisionProcess extends DecisionProcess {

	public DMSDecisionProcess(DecisionProcessState state,
						   PriorityQueue<DecisionProcessEvent> eventQueue,
						   RoutingPolicy routingPolicy,
						   Solution<TaskSeqRoute> plan)
	{
		super(state, eventQueue, routingPolicy, plan);
	}

	@Override
	public void run() {
		state.getInstance().setSeed(state.getSeed());

		while (!eventQueue.isEmpty()) {
			DecisionProcessEvent event = eventQueue.poll();
			event.trigger(this);
			ArrayList<DecisionSituation> recordedSituations = event.getRecordedSituations();
			if(!recordedSituations.isEmpty())
			{
				seenSituations.addAll(recordedSituations);
				recordedSituations.clear();
			}
		}
	}

	public static DMSReactiveDecisionProcess initDMSReactive(Instance instance,
													   long seed,
													   RoutingPolicy routingPolicy)
	{
		DecisionProcessState state = new DecisionProcessState(instance, seed);
		PriorityQueue<DecisionProcessEvent> eventQueue = new PriorityQueue<>();
		for (NodeSeqRoute route : state.getSolution().getRoutes())
			eventQueue.add(new ReactiveRefillEvent(0, route));

		return new DMSReactiveDecisionProcess(state, eventQueue, routingPolicy);
	}

	public ArrayList<DecisionSituation> getSeenSituations()
	{
		return seenSituations;
	}

	ArrayList<DecisionSituation> seenSituations = new ArrayList<>();
}
