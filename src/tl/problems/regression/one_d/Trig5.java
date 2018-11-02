package tl.problems.regression.one_d;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class Trig5 extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Trig5: cos(x) + sin(x) + sin(2x) + sin(3x) + sin(4x)";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return cos(x) + sin(x) + sin(2*x) + sin(3*x) + sin(3*x);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}