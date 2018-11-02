package tl.problems.regression.one_d;

import static java.lang.Math.pow;

public class Poly4 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly4: x^6 + 2x^5 + x^4 + 3x^3 + 2x^2 + 5x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return (2 * pow(x, 6)) + (2 * pow(x, 5)) + (pow(x, 4)) + (3 * pow(x, 3)) + (2 * pow(x, 2))
				+ (5 * x);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
