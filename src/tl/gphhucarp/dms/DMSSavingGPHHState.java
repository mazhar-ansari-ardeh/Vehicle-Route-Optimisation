package tl.gphhucarp.dms;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.GPHHEvolutionState;

import java.util.*;

public class DMSSavingGPHHState extends GPHHEvolutionState implements DMSSaver
{
	protected TreeMap<Individual, List<DecisionSituation>> seenSituations = new TreeMap<>();

	private List<ReactiveDecisionSituation> initialSituations = new ArrayList<>();
	private boolean saveDMS = true;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
	}

//	@Override
//	public void resetSeenSituations()
//	{
//		seenSituations.clear();
//	}

	private void updateDSMIfBetter(Individual ind, List<DecisionSituation> situations)
	{
		if(!this.saveDMS)
			return;
		final int MAX_SIZE = 2;
		if(seenSituations.size() < MAX_SIZE)
		{
			List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
			situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
			seenSituations.put(ind, clonedSituations);
			return;
		}
		if(ind.fitness.fitness() < seenSituations.lastKey().fitness.fitness())
		{
			List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
			situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
			seenSituations.put(ind, clonedSituations);
			seenSituations.remove(seenSituations.lastKey());
		}
	}

	@Override
	public void updateSeenSituations(Individual ind, List<DecisionSituation> situations)
	{
		if(!this.saveDMS)
			return;

		if(seenSituations.size() > 1)
			return;

		List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
		situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
		seenSituations.put(ind, clonedSituations);
		if(this.seenSituations.size() > 5)
		{
			seenSituations.remove(seenSituations.lastKey());
		}
	}

	@Override
	public List<ReactiveDecisionSituation> getAllSeenSituations()
	{
		ArrayList<ReactiveDecisionSituation> retval = new ArrayList<>();
		Set<Map.Entry<Individual, List<DecisionSituation>>> a = seenSituations.entrySet();
		for(Map.Entry<Individual, List<DecisionSituation>> e : a)
		{
			List<DecisionSituation> situations = e.getValue();
			for(DecisionSituation situation : situations)
			{
				ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
				if(rds.getPool().size() >= 2) // greater than two because the correlation-based phenotypics require it.
					retval.add(rds);
			}
		}

		for(Individual ind : seenSituations.keySet())
		{
			List<DecisionSituation> situations = seenSituations.get(ind);
			for(DecisionSituation situation : situations)
			{
				ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
				if(rds.getPool().size() > 0)
					retval.add(rds);
			}
		}

		return retval;
	}

	@Override
	public List<ReactiveDecisionSituation> getInitialSituations()
	{
		return initialSituations;
	}

	@Override
	public void addToInitialSituations(DecisionSituation initialSituation)
	{
		if(this.initialSituations.size()> 10000)
			return;
		this.initialSituations.add((ReactiveDecisionSituation) initialSituation);
	}

	@Override
	public void setDMSSavingEnabled(boolean enabled) {
		this.saveDMS = enabled;
	}

	@Override
	public boolean isDMSSavingEnabled() {
		return this.saveDMS;
	}

	protected void clear()
	{
//		if(!clear || seenSituations.size() == 0)
//		{
//			seenSituations.clear();
//			return;
//		}
//
//		int numDecisionSituations = 30;
//		long shuffleSeed = 8295342;
//
//		List<ReactiveDecisionSituation> allSeenSituations = getAllSeenSituations();
//		Collections.shuffle(allSeenSituations, new Random(shuffleSeed));
//		allSeenSituations = allSeenSituations.subList(0, numDecisionSituations);
//
//		SimpleNichingAlgorithm.clearPopulation(this, allSeenSituations, 0, 1);
//		allSeenSituations.clear();
//
//		// Clear the list so that new situations do not pile on the old ones. Don't know if this is a good idea.
//		seenSituations.clear();
	}
}