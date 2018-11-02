package tl.problems.regression.one_d;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

public class Trig3  extends RegressionProblem
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public String toString()
	{
		return "Trig3: cos(x) + sin(x) + sin(x)^2 + sin(x)^3 + sin(x)^4 + sin(x)^5";
	}

	@Override
	protected double doCalculation(double x)
	{
		evalCount++;
		return cos(x) + sin(x) + pow(sin(x), 2) + pow(sin(x), 3) + pow(sin(x), 4) + pow(sin(x), 5);
	}

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}
}