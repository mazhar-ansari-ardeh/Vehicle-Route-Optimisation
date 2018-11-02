package tl.problems.regression;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class Mul extends GPNode {

	private static final long serialVersionUID = 1L;

	public Mul() {
		super();
		children = new GPNode[2];
	}

	@Override
	public String toString()
	{
		return "*";
	}

	@Override
	public int expectedChildren()
	{
		return 2;
	}

	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual,
			Problem problem)
	{
		VectorData rd = (VectorData)input;

		children[0].eval(state, thread, rd, stack, individual, problem);;
		double result1 = rd.getResult();

		children[1].eval(state, thread, rd, stack, individual, problem);
		double result2 = rd.getResult();

		rd.setResult(result1 * result2);
	}

}
