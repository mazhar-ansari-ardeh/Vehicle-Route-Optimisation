package sandbox.tutorial2;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.vector.IntegerVectorIndividual;

public class AddSubtract extends Problem implements SimpleProblemForm 
{

	private static final long serialVersionUID = 1L;

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum) 
	{
		if(ind.evaluated)
			return; 
		
		if(! (ind instanceof IntegerVectorIndividual))
			state.output.fatal("Whoa!  It's not a IntegerVectorIndividual!!!",null);
		
		IntegerVectorIndividual ind2 = (IntegerVectorIndividual) ind; 
		if (!(ind2.fitness instanceof SimpleFitness))
            state.output.fatal("Whoa!  It's not a SimpleFitness!!!",null);
		
		int rawFitness = 0;
		for(int i = 0; i < ind2.genome.length; i++)
		{
			if(i % 2 == 0)
				rawFitness += ind2.genome[i];
			else
				rawFitness -= ind2.genome[i];
		}
		
		if(rawFitness < 0)
			rawFitness = -rawFitness; 
		
		((SimpleFitness)ind2.fitness).setFitness(state, rawFitness / ind2.genome.length, false);
		ind2.evaluated = true; 
	}

}
