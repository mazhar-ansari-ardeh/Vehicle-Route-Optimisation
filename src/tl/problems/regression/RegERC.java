package tl.problems.regression;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.ERC;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Code;
import ec.util.DecodeReturn;

public class RegERC extends ERC
{
	private static final long serialVersionUID = 1L;

	// TODO: Do not hardcode this.
	public static final double max = +1;

	public static final double min = -1;

	public double value;

	@Override
	public boolean decode(DecodeReturn ret)
	{
		int pos = ret.pos;
		String data = ret.data;
		Code.decode(ret);
		if (ret.type != DecodeReturn.T_DOUBLE) // uh oh! Restore and signal error.
		{
			ret.data = data; ret.pos = pos; return false;
		}
		value = ret.d;
		return true;
	}

	@Override
	public String toString()
	{
		return Double.toString(value);
	}

	@Override
	public String toStringForHumans()
	{
		return Double.toString(value);
	}

	@Override
	public String encode()
	{
		return Code.encode(value);
	}

	@Override
	public void resetNode(EvolutionState state, int thread)
	{
		value = min + (max - min) * state.random[thread].nextDouble();
	}

	@Override
	public boolean nodeEquals(GPNode node)
	{
		return (node.getClass() == this.getClass() && ((RegERC)node).value == value);
	}

	@Override
	public void readNode(EvolutionState state, DataInput input) throws IOException
	{
		value = input.readDouble();
	}

	@Override
	public void writeNode(EvolutionState state, DataOutput output) throws IOException
	{
		output.writeDouble(value);
	}

	public void mutateNode(EvolutionState state, int thread)
	{
		double v;
		do v = value + state.random[thread].nextGaussian() * 0.01;
		while( v < 0.0 || v >= 1.0 );
		value = v;
	}


	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual,
			Problem problem)
	{
		if(!(input instanceof VectorData))
			state.output.fatal("Received an input value of unknown type: " + input.getClass());

		((VectorData)input).setResult(value);
	}

}
