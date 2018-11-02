package tl.problems.regression;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class Sin extends GPNode
{
	private static final long serialVersionUID = 1L;

	public Sin() {
		super();
		children = new GPNode[1];
	}

	@Override
	public String toString()
	{
		return "sin";
	}

	@Override
	public int expectedChildren()
	{
		return 1;
	}

	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack,
			GPIndividual individual, Problem problem)
	{
		VectorData rd = (VectorData)input;

		children[0].eval(state, thread, rd, stack, individual, problem);
		double result1;
		result1 = rd.getResult();

		rd.setResult(Math.sin(result1));
	}
}
