package sandbox.tutorial5;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

public class SingleValuedRegression extends GPProblem implements SimpleProblemForm
{
	private static final long serialVersionUID = 1L;

	public static final String P_DATA = "data";

	public double currentX;

	public static final double[] X = {-2.00000, -1.75000, -1.50000, -1.25000, -1.00000, -0.75000, -0.50000,
			-0.25000, 0.00000, 0.25000, 0.50000, 0.75000, 1.00000, 1.25000, 1.50000, 1.75000, 2.00000,
			2.25000, 2.50000, 2.75000};

	public static final double[] Y = {37.00000, 24.16016, 15.06250, 8.91016, 5.00000, 2.72266, 1.56250, 1.09766,
			1.00000, 1.03516, 1.06250, 1.03516, 1.00000, 1.09766, 1.56250, 2.72266, 5.00000, 8.91016, 15.06250,
			24.16016};

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		if(!(input instanceof DoubleData))
		{
			state.output.fatal("GPData class must subclass from" + DoubleData.class, base.push(P_DATA), null);
		}
	}

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum)
	{
		if(ind.evaluated)
			return;

		DoubleData input = (DoubleData)this.input;
		int hits = 0;
		double sum = 0;
// 		double expectedResult = 0;
		double result;

		for(int i = 0; i < X.length; i++)
		{
			currentX = X[i];
			((GPIndividual)ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual)ind, this);
			result = Math.abs(input.x - Y[i]);
			if(result < 0.01)
			{
				hits++;
			}

			sum += result;
		}

		KozaFitness f = ((KozaFitness)ind.fitness);
		f.hits = hits;
		f.setStandardizedFitness(state, sum);
		ind.evaluated = true;
	}

}
