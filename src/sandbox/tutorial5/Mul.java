package sandbox.tutorial5;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class Mul extends GPNode {

	private static final long serialVersionUID = 1L;

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
		DoubleData rd = (DoubleData)input; 
		double child0Result;
		
		children[0].eval(state, thread, rd, stack, individual, problem);;
		child0Result = rd.x; 
		
		children[1].eval(state, thread, rd, stack, individual, problem);
		rd.x *= child0Result; 
	}

}
