package tl.problems.regression.one_d;

import static java.lang.Math.pow;

public class Poly01 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly01: x^3 + 2x^2 + 3x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return pow(x, 3) + (2 * pow(x, 2)) + (3 * x);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
