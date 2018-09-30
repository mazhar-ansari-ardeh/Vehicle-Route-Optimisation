package tl.knowledge.codefragment.fitted;

import ec.gp.GPNode;
import tl.knowledge.codefragment.CodeFragmentKI;

public class FittedCodeFragment extends CodeFragmentKI
{
	double fitness;
	
	boolean fitnessIsSet = false;

	public FittedCodeFragment(GPNode codeFragment, double fitness)
	{
		super(codeFragment);
		this.fitness = fitness;
		fitnessIsSet = true; 
	}
	
	public FittedCodeFragment(GPNode codeFragment) 
	{
		super(codeFragment);
		fitnessIsSet = false; 
	}
}