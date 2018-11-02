package tl.problems.regression.one_d;

import static java.lang.Math.pow;

public class Poly5 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Poly4: x^7 + 2x^6 + 3x^5 + 4x^4 + x^3 + x^2 + 3x";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return pow(x, 7) + (2 * pow(x, 6)) + (3 * pow(x, 5)) + (4 * pow(x, 4)) + (pow(x, 3))
						 + pow(x, 2) + (3 * x);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
