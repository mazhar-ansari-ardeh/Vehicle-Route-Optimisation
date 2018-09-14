package sandbox.tutorial6.nodes;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import sandbox.tutorial6.VectorData;

public class Class4 extends GPNode 
{
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() 
	{
		return "Class4";
	}
	
	@Override
	public int expectedChildren() 
	{
		return 0;
	}

	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual,
			Problem problem) 
	{
		VectorData rd = ((VectorData)(input));
        rd.mDataClass = 4;
	}

}
