package sandbox.tutorial4;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class Add extends GPNode
{
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() 
	{
		return "+";
	}
	
	@Override
	public int expectedChildren() 
	{
		return 2;
	}

	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, 
			GPIndividual individual, Problem problem) 
	{
		double result; 
		DoubleData rd = (DoubleData)input;
		
		children[0].eval(state, thread, rd, stack, individual, problem);
		result = rd.x;
		
		children[1].eval(state, thread, rd, stack, individual, problem);;
		rd.x += result;  
	}
}
