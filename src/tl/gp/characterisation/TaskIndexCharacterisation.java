package tl.gp.characterisation;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.List;

/**
 * This characterisation method makes its decision based on a list of decision situations. For each situation, S, it
 * considers the set of tasks, TASKS, inside the S, asks the routing policy to select one of TASKS and finds the
 * index of the selected task inside TASKS and considers that as the characterisation of the rule for the situation S.
 *
 * This is based on the code that Shaolin gave me. Although it does not make much sense, the Euclidean distance measure
 * based on this methods and the surrogate method based on it work very well.
 */
public class TaskIndexCharacterisation implements RuleCharacterisation<int[]>
{
	private final List<ReactiveDecisionSituation> decisionSituations;

	public TaskIndexCharacterisation(final List<ReactiveDecisionSituation> dps)
	{
		if(dps == null || dps.isEmpty())
			throw new IllegalArgumentException("List of decision situations cannot be null or empty: " + dps);
		this.decisionSituations = dps;
	}

	public int[] characterise(RoutingPolicy rule)
	{
		int dsSize = this.decisionSituations.size();
		final int[] charList = new int[dsSize];
		for (int i = 0; i < dsSize; i++) {
			final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
			final Arc op = rule.next(situation);
			final int index = situation.getPool().indexOf(op);

			charList[i] = index;
		}
		return charList;
	}
}
