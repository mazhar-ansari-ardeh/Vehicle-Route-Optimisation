package tl.gphhucarp;

import ec.EvolutionState;
import ec.Individual;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import tl.gp.FitnessUtils;
import tl.gp.fitness.NormalisedMultiObjectiveFitness;

/**
 * This class is a simple extension of the ReactiveGPHHProblem that uses that class for fitness evaluation but
 * normalises the fitness value of the GP individual with the fitness value that {@link PathScanning5Policy} provides.
 */
public class NormalisedReactiveGPHHProblem extends ReactiveGPHHProblem
{
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);
	}

	private double[] psFitness;
	private int genPSFitnessUpdated = -1;

	private void normalize(EvolutionState state, Individual indi)
	{
		if(genPSFitnessUpdated != state.generation)
		{
			PathScanning5Policy psPolicy = new PathScanning5Policy(new SimpleTieBreaker());
			MultiObjectiveFitness psFitness = FitnessUtils.create(1);
			evaluationModel.evaluate(psPolicy, null, psFitness, state);
			this.psFitness = psFitness.objectives.clone();
			genPSFitnessUpdated = state.generation;
		}
		if(indi.fitness instanceof NormalisedMultiObjectiveFitness)
		{
			NormalisedMultiObjectiveFitness nFit = (NormalisedMultiObjectiveFitness) indi.fitness;
			nFit.setReferenceFitness(psFitness);
		}
		else
			indi.fitness = new NormalisedMultiObjectiveFitness(indi.fitness, psFitness);
	}

	public void evaluate(EvolutionState state, Individual indi, int subpopulation, int threadnum)
	{
		super.evaluate(state, indi, subpopulation, threadnum);

		normalize(state, indi);
	}
}
