package tl.gphhucarp.dms;

import ec.Individual;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.GPHHEvolutionState;
import tl.gp.niching.SimpleNichingAlgorithm;

import java.util.*;

public interface DMSSaver
{
	void resetSeenSituations();

	void updateSeenSituations(Individual ind, List<DecisionSituation> situations);

	List<ReactiveDecisionSituation> getAllSeenSituations();

	List<ReactiveDecisionSituation> getInitialSituations();

	void addToInitialSituations(DecisionSituation initialSituation);
}