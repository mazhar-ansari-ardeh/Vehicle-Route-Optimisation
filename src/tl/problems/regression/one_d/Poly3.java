package tl.problems.regression.one_d;

import static java.lang.Math.pow;

public class Poly3 extends RegressionProblem
{
// 2x5 + 3x4 + 2x3 + 3x2 + x
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly3: 2x^5 + 3x^4 + 2x^3 + 3x^2 + x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return (2 * pow(x, 5)) + (3 * pow(x, 4)) + (2 * pow(x, 3)) + (3 * pow(x, 2)) + x;
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
