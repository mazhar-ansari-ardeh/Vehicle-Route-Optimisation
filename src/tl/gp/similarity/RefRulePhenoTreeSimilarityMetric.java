package tl.gp.similarity;

import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPTree;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import tl.gp.characterisation.RefRuleCharacterisation;

import java.util.HashMap;
import java.util.List;

public class RefRulePhenoTreeSimilarityMetric implements SituationBasedTreeSimilarityMetric
{

	private final RoutingPolicy referenceRule;
	RefRuleCharacterisation ch;

	public RefRulePhenoTreeSimilarityMetric(Individual referenceRule)
	{
		this((GPIndividual)referenceRule);
	}

	public RefRulePhenoTreeSimilarityMetric(GPIndividual referenceRule)
	{
		this(new GPRoutingPolicy(new IdentityPoolFilter(), referenceRule.trees[0]));
	}

	public RefRulePhenoTreeSimilarityMetric()
	{
		this(new PathScanning5Policy());
	}

	public RefRulePhenoTreeSimilarityMetric(RoutingPolicy referenceRule)
	{
		this.referenceRule = referenceRule;
	}

	@Override
	public String getName()
	{
		return "RefRule";
	}

	@Override
	public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
	{
		if(ch == null)
			throw new RuntimeException("Situations are not set.");
		int[] ch1 = characterise(tree1);
		int[] ch2 = characterise(tree2);

		return distance(ch1, ch2);
	}

	public static double distance(final int[] charList1, final int[] charList2) {
		double distance = 0.0;
		for (int i = 0; i < charList1.length; ++i) {
			final double diff = charList1[i] - charList2[i];
			distance += diff * diff;
		}
		return Math.sqrt(distance);
	}

	private final HashMap<GPTree, int[]> cache = new HashMap<>();

	public int[] characterise(GPRoutingPolicy tree)
	{
		synchronized (cache)
		{
			int CACHE_SIZE = 10000;
			if(cache.size() > CACHE_SIZE)
				cache.clear();
			return cache.computeIfAbsent(tree.getGPTree(), t -> ch.characterise(tree));
		}
	}

	public int[] characterise(GPIndividual ind, int tree, PoolFilter filter)
	{
		synchronized (cache)
		{
			int CACHE_SIZE = 10000;
			if(cache.size() > CACHE_SIZE)
				cache.clear();
			return cache.computeIfAbsent(ind.trees[tree], t -> ch.characterise(ind, tree, filter));
		}
	}

	@Override
	public void setSituations(List<ReactiveDecisionSituation> situations)
	{
		ch = new RefRuleCharacterisation(situations, this.referenceRule);
		synchronized (cache)
		{
			cache.clear();
		}
	}
}


