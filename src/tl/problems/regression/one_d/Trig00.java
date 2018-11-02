package tl.problems.regression.one_d;

import static java.lang.Math.pow;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Trig00 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Trig00: cos(x) + sin(x)^2";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return cos(x) + pow(sin(x), 2);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}
