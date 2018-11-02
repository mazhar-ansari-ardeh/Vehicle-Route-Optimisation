package tl.problems.regression.one_d;

import static java.lang.Math.pow;

import ec.simple.SimpleProblemForm;

public class Poly0 extends RegressionProblem implements SimpleProblemForm
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly0: x^4 + x^3 + x^2 + x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return pow(x, 4) + pow(x, 3) + pow(x, 2) + x;
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}

}
