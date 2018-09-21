package tutorial7.problems.regression.one_d;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPProblem;
import ec.util.Parameter;
import tutorial7.problems.regression.VectorData;

public abstract class RegressionProblem extends GPProblem
{

	private static final long serialVersionUID = 1L;
	public static final String P_DATA = "data";
	/**
	 * Number of tests of each individual in the population.
	 * For each text, a random input will be generated and given to
	 * the individual to evaluate and the result of evaluation will be
	 * compared with the actual result calculated with the actual
	 * function.
	 */
	public final int N_TESTS = 100;
	public static final String P_RANGE_MIN = "range_min";
	public static final String P_RANGE_MAX = "range_max";
	public static final String P_NUM_TESTS = "number-of-tests";
	public static double rangeMin;
	public static double rangeMax;
	public static double numTests;
//	protected String myBaseParameter = "";
	public static final double RANGE_MIN = -1;
	public static final double RANGE_MAX = +1;

	/**
	 * Keeps record of the number of times that that the <code>evaluate</code> function has been
	 * invoked.
	 */
	protected int evalCout = 0;

	public int getEvaluationCount()
	{
		return evalCout;
	}

//	/**
//	 * Because this class needs to keep track of the number of times that its evaluation method is
//	 * invoked, it does not support cloning, which in the context of ECJ, resets this counter.
//	 * Invoking this method will throw an <code>UnsupportedOperationException</code>.
//	 */
//	@Override
//	public Object clone()
//	{
//		throw new UnsupportedOperationException("This class does not support cloning.");
//	}

	public RegressionProblem()
	{
		super();
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		if(!(input instanceof VectorData))
		{
			state.output.fatal("GPData class must subclass from" + VectorData.class,
					base.push(P_DATA), null);
		}

		// base is 'eval.problem'
		// result of base.push(myBaseParameter) will be 'eval.problem.$myBaseParameter'
		Parameter p = base.push(getMyBaseParameter()).push(P_RANGE_MIN);
		rangeMin = state.parameters.getIntWithDefault(p, null, -1);

		p = base.push(P_RANGE_MAX);
		rangeMax = state.parameters.getIntWithDefault(p, null, +1);

		p = base.push(P_NUM_TESTS);
		numTests = state.parameters.getIntWithDefault(p, null, 100);
		if(numTests <= 0)
			state.output.fatal("Number of tests must be greater than zero. " + VectorData.class
					, base.push(P_NUM_TESTS), null);
	}

	/**
	 * This method is empty and must be implemented by classes that inherit this class.
	 */
	public void evaluate(EvolutionState state, Individual ind, int subpopulation,
			int threadnum)
	{

	}

	/**
	 * The name of the parameter that this class uses to load parameters that are specific to this
	 * class.
	 */
	public String getMyBaseParameter()
	{
		return null;
	}

}