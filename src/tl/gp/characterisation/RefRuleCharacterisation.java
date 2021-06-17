package tl.gp.characterisation;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.List;

/**
 * This class implements the characterisation method proposed in the 'On Using Surrogate ...' paper by Juergen.
 */
public class RefRuleCharacterisation implements RuleCharacterisation<int[]>
{
	private final List<ReactiveDecisionSituation> decisionSituations;
	private final RoutingPolicy referenceRule;
	private List<int[]> referenceRanks;

	public RefRuleCharacterisation(final List<ReactiveDecisionSituation> dps, RoutingPolicy refRule)
	{
		if(dps == null || dps.isEmpty())
			throw new IllegalArgumentException("List of decision situations cannot be null or empty: " + dps);
		if(refRule == null)
			throw new IllegalArgumentException("The reference rule cannot be null.");

		this.decisionSituations = dps;
		this.referenceRule = refRule;
		calcReferenceIndexes();
	}

	@Override
	public int[] characterise(RoutingPolicy policy)
	{
		final int[] charList = new int[this.decisionSituations.size()];
		for (int i = 0; i < this.decisionSituations.size(); i++)
		{
			final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
			final Arc op = policy.next(situation);
			if(op == null)
			{
				charList[i] = situation.getPool().size()+1;
				continue;
			}
			final int index = situation.getPool().indexOf(op);
			charList[i] = this.referenceRanks.get(i)[index];
		}
		return charList;
	}

	private void calcReferenceIndexes()
	{
		TaskRankCharacterisation ranker = new TaskRankCharacterisation(this.decisionSituations);
		this.referenceRanks = ranker.characterise(this.referenceRule);
	}
}
