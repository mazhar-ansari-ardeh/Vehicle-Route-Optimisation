package tutorial7;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class X extends GPNode
{
	private static final long serialVersionUID = 1L;

	@Override
	public String toString()
	{
		return "X";
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
		TripletData rd = ((TripletData)(input));
		rd.setResult(rd.getX());
//        rd.x = ((MultiValuedRegression)problem).currentX;
	}

}
