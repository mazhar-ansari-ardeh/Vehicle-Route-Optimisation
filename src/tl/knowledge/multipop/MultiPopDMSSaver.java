package tl.knowledge.multipop;

import ec.Individual;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.List;

public interface MultiPopDMSSaver extends tl.gphhucarp.dms.DMSSaver
{
    void updateSeenSituations(int subpop, Individual ind, List<DecisionSituation> situations);

    List<ReactiveDecisionSituation> getAllSeenSituations(int subpop);
}