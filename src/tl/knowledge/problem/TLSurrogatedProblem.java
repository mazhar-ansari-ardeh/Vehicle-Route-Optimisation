package tl.knowledge.problem;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import tl.knowledge.surrogate.knn.EnsembleSurrogateFitness;

public class TLSurrogatedProblem extends GPProblem implements SimpleProblemForm
{
	EnsembleSurrogateFitness surrogateFitness;

	int currentGen = 0;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
	}

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum)
	{
		if(state.generation != currentGen)
		{
//			surrogateFitness.updateSurrogatePool();
//			surrogateFitness.updateWeights();
			currentGen = state.generation;
			// prevGen.clear()
		}
		surrogateFitness.fitness(ind);
	}
}
