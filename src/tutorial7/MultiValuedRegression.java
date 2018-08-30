package tutorial7;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

public class MultiValuedRegression extends GPProblem implements SimpleProblemForm
{
	private static final long serialVersionUID = 1L;

	public static final String P_DATA = "data";

	/**
	 * Number of tests of each individual in the population.
	 * For each text, a random input will be generated and given to
	 * the individual to evaluate and the result of evaluation will be
	 * compared with the actual result calculated with the actual
	 * function.
	 */
	public final int N_TESTS = 20;

	public static final double RANGE_MIN = -1;
	public static final double RANGE_MAX = +1;

	public double currentX;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		if(!(input instanceof TripletData))
		{
			state.output.fatal("GPData class must subclass from" + TripletData.class, base.push(P_DATA), null);
		}
	}

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum)
	{
		if(ind.evaluated)
			return;

		int hits = 0;
		double sum = 0;
		double result;

		double step = 0.5;
		for(double x = RANGE_MIN; x < RANGE_MAX; x += step)
			for(double y = RANGE_MIN; y < RANGE_MAX; y += step)
				for(double z = RANGE_MIN; z < RANGE_MAX; z += step)
		{
			TripletData input = new TripletData(x, y, z);

			((GPIndividual)ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual)ind, this);
			double expectedResult = Math.pow(x + y + z, 3);
			result = Math.pow(input.getResult() - expectedResult, 2);
			if(result < 0.01)
			{
				hits++;
			}

			sum += result;
		}

		KozaFitness f = ((KozaFitness)ind.fitness);
		f.hits = hits;
		f.setStandardizedFitness(state, Math.sqrt(sum));
		ind.evaluated = true;
	}

}
