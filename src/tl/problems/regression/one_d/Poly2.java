package tl.problems.regression.one_d;

import static java.lang.Math.pow;


public class Poly2 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly2: 2x^4 + 3x^3 + 2x^2 + x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return 2 * pow(x, 4) + 3 * pow(x, 3) + 2 * pow(x, 2) + x;
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
