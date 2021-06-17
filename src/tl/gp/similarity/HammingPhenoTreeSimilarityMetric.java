package tl.gp.similarity;

import ec.gp.GPIndividual;
import ec.gp.GPTree;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.characterisation.TaskIndexCharacterisation;

import java.util.HashMap;
import java.util.List;

public class HammingPhenoTreeSimilarityMetric implements SituationBasedTreeSimilarityMetric {

	TaskIndexCharacterisation ch;

	@Override
	public String getName() {
		return "Hamming";
	}

	@Override
	public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
	{
		if(ch == null)
			throw new RuntimeException("Situations are not set.");
		int[] ch1 = characterise(tree1);
		int[] ch2 = characterise(tree2);

		double distance = 0;
		for (int i = 0; i < ch1.length; i++)
			distance += (ch1[i] == ch2[i]) ? 0 : 1;

		return distance;
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
		ch = new TaskIndexCharacterisation(situations);
		synchronized (cache)
		{
			cache.clear();
		}
	}
}
