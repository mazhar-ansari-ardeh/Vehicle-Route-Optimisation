package tl.knowledge.sst;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.sst.lsh.Vector;
import tl.knowledge.sst.lsh.families.EuclidianHashFamily;
import tl.knowledge.sst.lsh.families.HashFamily;
import tl.knowledge.surrogate.lsh.FittedVector;

import java.util.List;
import java.util.stream.Collectors;

public class LSHSSTEvolutionState extends SSTEvolutionState
{
	/**
	 * The individuals that are transferred from the source domain. This list is loaded once and is not updated
	 * afterwards.
	 */
	private LSH transferredLHS;

	/**
	 * The individuals that are discovered in the target domain during the GP evolution in the target domain. This list
	 * is updated after each generation.
	 */
	private LSH discoveredInds;

	TaskIndexCharacterisation trc;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		trc = new TaskIndexCharacterisation(
				getInitialSituations().subList(0, Math.min(dmsSize, getInitialSituations().size())));

		final int NUM_HASHES = 10;
		final int NUM_HASH_TABLES = 100;
		discoveredInds = setUpLSH((int) historySimThreshold, dmsSize, NUM_HASHES, NUM_HASH_TABLES);

		List<GPRoutingPolicy> transferredPolicies = getTransferredIndividuals();
		List<GPIndividual> transferredInds = transferredPolicies.stream().map(policy -> policy.getGPTree().owner).collect(Collectors.toList());
		transferredLHS = setUpLSH((int) historySimThreshold, dmsSize, NUM_HASHES, NUM_HASH_TABLES);
		updateLSH(transferredLHS, transferredInds);

//		tempLSH = setUpLSH((int) historySimThreshold, dmsSize, NUM_HASHES, NUM_HASH_TABLES);
	}

	@Override
	boolean isNew(Individual i, double similarityThreshold)
	{
		if(i == null)
			throw new RuntimeException("The individual cannot be null.");
		if(isSeenIn(i, tempInds, similarityThreshold))
			return false;
		if(isSeenIn(transferredLHS, i))
		{
			return false;
		}
		boolean isSeen = false;
		if(enableEvoHistUpdate)
		{
			isSeen = isSeenIn(discoveredInds, i);
			if (!isSeen)
			{
				GPIndividual j = (GPIndividual) i.clone();
				// GP builders do not create individuals but GP nodes. As a result, cloning their product will not matter
				// because they their fitness will not be updated after evaluation.
				tempInds.add(new GPRoutingPolicy(filter, j.trees[0]));
			}
		}

		return !isSeen;
	}

	private boolean isSeenIn(Individual i, List<GPRoutingPolicy> pool, double similarityThreshold)
	{
		if(pool.isEmpty())
			return false;

		GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0]);

		for (GPRoutingPolicy gpRoutingPolicy : pool)
		{
			if (metrics.distance(gpRoutingPolicy, policy) <= similarityThreshold)
				return true;
		}

		return false;
	}

	private boolean isSeenIn(LSH database, Individual i)
	{
		assert database != null;

		GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0]);
		int[] characterise = trc.characterise(policy);

		List<Vector> query = database.query(new Vector(characterise), 1);
		return query.size() > 0;
	}

	@Override
	protected void updateSearchHistory(Individual[] inds)
	{
		if(!enableEvoHistUpdate)
			return;

		for (Individual ind : inds)
		{
			SSTIndividual i = (SSTIndividual) ind;
			if (isSeenIn(transferredLHS, ind))
			{
				i.setOrigin(IndividualOrigin.InitTransfer);
				continue;
			}
			if (isSeenIn(discoveredInds, i))
			{
				continue;
			}
			if(i.getOrigin() == null) // The origin could be crossover, mutation, ....
				i.setOrigin(IndividualOrigin.InitRandom);
			updateLSH(discoveredInds, i);
		}
	}

	private LSH setUpLSH(int radius, int dimensions, int numberOfHashes, int numberOfHashTables)
	{
		final int W = 10;
		int w = W * radius;
		w = w == 0 ? 1 : w;
		HashFamily family = new EuclidianHashFamily(w, dimensions);
		LSH lsh = new LSH(family);
		lsh.buildIndex(numberOfHashes, numberOfHashTables, this.random[0]);

		return lsh;
	}

	private void updateLSH(LSH lsh, Individual ind)
	{
		GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual) ind).trees[0]);
		int[] characterise = trc.characterise(policy);

		lsh.add(new FittedVector(characterise, ind.fitness.fitness(), generation));
	}

	private void updateLSH(LSH lsh, List<GPIndividual> inds)
	{
		for(GPIndividual ind : inds)
		{
			GPRoutingPolicy policy = new GPRoutingPolicy(filter, ind.trees[0]);
			int[] characterise = trc.characterise(policy);

			lsh.add(new FittedVector(characterise, ind.fitness.fitness(), generation));
		}
	}
}
