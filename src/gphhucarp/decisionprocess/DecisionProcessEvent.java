package gphhucarp.decisionprocess;

import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract decision process event.
 * It has a time that the event occurs.
 * Natural comparison prefers the earlier event.
 *
 * Created by gphhucarp on 28/08/17.
 */
public abstract class DecisionProcessEvent implements Comparable<DecisionProcessEvent> {
    protected double time;

    public ArrayList<DecisionSituation> getRecordedSituations()
    {
        return recordedSituations;
    }

    protected ArrayList<DecisionSituation> recordedSituations = new ArrayList<>();

    public DecisionProcessEvent(double time) {
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    public abstract void trigger(DecisionProcess decisionProcess);

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

    @Override
    public int compareTo(DecisionProcessEvent o) {
        if (time < o.time)
            return -1;

        if (time > o.time)
            return 1;

        return 0;
    }
}
