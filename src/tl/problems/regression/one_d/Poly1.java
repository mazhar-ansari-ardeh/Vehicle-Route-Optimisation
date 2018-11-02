package tl.problems.regression.one_d;

import static java.lang.Math.pow;

public class Poly1 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly1: x^6 + x^5 + x^4 + x^3 + x^2 + x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return pow(x, 6) + pow(x, 5) + pow(x, 4) + pow(x, 3) + pow(x, 2) + x;
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
