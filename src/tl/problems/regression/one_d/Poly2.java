package tl.problems.regression.one_d;

import static java.lang.Math.pow;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.koza.KozaFitness;
import tl.problems.regression.VectorData;

public class Poly2 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum)
	{
		if(ind.evaluated)
			return;

		int hits = 0;
		double sum = 0;
		double result;

		double step = (rangeMax - rangeMin) / numTests;
		for(double x = rangeMin; x < rangeMax; x += step)
		{
			VectorData input = new VectorData(x);

			((GPIndividual)ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual)ind, this);
			double expectedResult = 2 * pow(x, 4) + 3 * pow(x, 3) + 2 * pow(x, 2) + x;
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

	/**
	 * The name of the parameter that this class uses to load parameters that are specific to this
	 * class.
	 */
	@Override
	public String getMyBaseParameter()
	{
		return "poly2";
	}
}
