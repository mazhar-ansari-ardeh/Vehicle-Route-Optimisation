package tl.gp.niching;


import gphhucarp.core.Arc;
//import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

//import java.util.ArrayList;
import java.util.List;

public class PhenoCharacterisation
{
    private RoutingPolicy rp;
    private RoutingPolicy referenceRule;
    private List<ReactiveDecisionSituation> decisionSituations;
    private int[] referenceIndexes;
//    private DecisionProcess decisionProcess;

    public PhenoCharacterisation(final List<ReactiveDecisionSituation> dps, final RoutingPolicy referenceRule) {
//        this.decisionProcess = null;
        this.decisionSituations = dps;
        this.referenceRule = referenceRule;
        this.referenceIndexes = new int[this.decisionSituations.size()];
        this.calcReferenceIndexes();
    }

    public PhenoCharacterisation(final List<ReactiveDecisionSituation> dps) {
//        this.decisionProcess = null;
        this.decisionSituations = dps;
        this.referenceRule = null;
        this.referenceIndexes = new int[this.decisionSituations.size()];
//        this.calcReferenceIndexes();
    }

//    public List<ReactiveDecisionSituation> getDecisionSituations() {
//        return this.decisionSituations;
//    }
//
//    public RoutingPolicy getReferenceRule() {
//        return this.referenceRule;
//    }

//    public void setReferenceRule(final RoutingPolicy rule) {
//        this.referenceRule = rule;
//        this.calcReferenceIndexes();
//    }

    public int[] characterise(final RoutingPolicy rule) {
        final int[] charList = new int[this.decisionSituations.size()];
        for (int i = 0; i < this.decisionSituations.size(); i++) {
            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
            final List<Arc> queue = (List<Arc>)situation.getPool();
            final int refIdx = this.referenceIndexes[i]; // Warning: This implementation does not consider the reference rule
            final Arc op = rule.next(situation);
            final int rank = situation.getPool().indexOf(op);
            charList[i] = rank;
        }
        return charList;
    }

    public static double distance(final int[] charList1, final int[] charList2) {
        double distance = 0.0;
        for (int i = 0; i < charList1.length; ++i) {
            final double diff = charList1[i] - charList2[i];
            distance += diff * diff;
        }
        return Math.sqrt(distance);
    }

    private void calcReferenceIndexes() {
        for (int i = 0; i < this.decisionSituations.size(); ++i) {
            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
            final Arc op = this.referenceRule.next(situation);
            final int index = situation.getPool().indexOf(op);
            this.referenceIndexes[i] = index;
        }
    }

//    public List<ReactiveDecisionSituation> getDecisionSituations() {
//        return this.decisionSituations;
//    }
//
//    public RoutingPolicy getReferenceRule() {
//        return this.referenceRule;
//    }
//
//    public int[] getReferenceIndexes() {
//        return this.referenceIndexes;
//    }
}
