package tutorial3;

import ec.EvolutionState;
import ec.Individual;
import ec.Problem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.vector.DoubleVectorIndividual;

public class OddRosenbrock extends Problem implements SimpleProblemForm 
{

	private static final long serialVersionUID = 1L;

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum) 
	{
		if( !( ind instanceof DoubleVectorIndividual ) )
			state.output.fatal( "The individuals for this problem should be DoubleVectorIndividuals." );

		double[] genome = ((DoubleVectorIndividual)ind).genome;
		int len = genome.length;
		double value = 0;

		// Compute the Rosenbrock function for our genome
		for( int i = 1 ; i < len ; i++ )
			value += 100*(genome[i-1]*genome[i-1]-genome[i])*
			(genome[i-1]*genome[i-1]-genome[i]) +
			(1-genome[i-1])*(1-genome[i-1]);

		// Rosenbrock is a minimizing function which does not drop below 0. 
		// But SimpleFitness requires a maximizing function -- where 0 is worst
		// and 1 is best.  To use SimpleFitness, we must convert the function.
		// This is the Koza style of doing it:

		value = 1.0 / ( 1.0 + value );
		((SimpleFitness)(ind.fitness)).setFitness( state, value, value==1.0 );

		ind.evaluated = true;
	}

}
