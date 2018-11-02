package tl.problems.regression.one_d;

import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.cos;

public class Trig1 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Trig1: cos(x) + sin(x) + sin(x)^2 + sin(x)^3";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return cos(x) + sin(x) + pow(sin(x), 2) + pow(sin(x), 3);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}