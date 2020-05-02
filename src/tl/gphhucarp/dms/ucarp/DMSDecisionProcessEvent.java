package tl.gphhucarp.dms.ucarp;

import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionProcessEvent;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.ArrayList;

/**
 * This is an extension that allows saving decision situations seen while serving tasks.
 */
public abstract class DMSDecisionProcessEvent extends DecisionProcessEvent
{
	public ArrayList<DecisionSituation> getRecordedSituations()
	{
		return recordedSituations;
	}

	protected ArrayList<DecisionSituation> recordedSituations = new ArrayList<>();

	public DMSDecisionProcessEvent(double time)
	{
		super(time);
	}

	/**
	 * Record the decision situation if a decision is to be made in this event.
	 * Add this decision situation to the list.
	 * @param decisionSituation the decision situations to store the records.
	 */
	public void recordDecisionSituation(DecisionSituation decisionSituation) {
		// default do nothing
		if(decisionSituation != null)
			recordedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation)decisionSituation));
//            recordedSituations.add(((ReactiveDecisionSituation)decisionSituation));
	}
}
