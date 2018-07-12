package tutorial6.nodes;

import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tutorial6.VectorData;

public class AttributeNode extends GPNode {

	private static final long serialVersionUID = 1L;
	int mAttributeIndex;
	double mThreshold;
	
	
	public AttributeNode(int attributeIndex, double threshold) 
	{
		mAttributeIndex = attributeIndex;
		mThreshold = threshold; 
	}

	@Override
	public String toString() 
	{
		return "Attrib" + mAttributeIndex + " < " + mThreshold;
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
		
		if(rd.vector[mAttributeIndex] < mThreshold)
		{
			children[0].eval(state, thread, rd, stack, individual, problem);
		}
		else
			children[1].eval(state, thread, rd, stack, individual, problem);
	}

}
