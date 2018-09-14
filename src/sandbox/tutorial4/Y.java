package sandbox.tutorial4;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class Y extends GPNode 
{
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() 
	{
		return "Y";
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
		DoubleData rd = ((DoubleData)(input));
        rd.x = ((MultiValuedRegression)problem).currentY;
	}

}
