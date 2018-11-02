package tl.problems.regression.one_d;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

public class Trig01 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Trig01: sin(x) + sin(2x) + sin(x)^2";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return sin(x) + sin(2 * x) + pow(sin(x), 2);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
