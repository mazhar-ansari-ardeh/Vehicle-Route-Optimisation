package tl.knowledge.codefragment.fitted;

import java.io.Serializable;

import ec.gp.GPNode;
import tl.knowledge.codefragment.CodeFragmentKI;

@Deprecated
public class FittedCodeFragment extends CodeFragmentKI implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Double fitness = null;

	// boolean fitnessIsSet = false;

	public FittedCodeFragment(GPNode codeFragment, Double fitness)
	{
		super(codeFragment);
		this.fitness = fitness;
		// fitnessIsSet = true;
	}

	public FittedCodeFragment(GPNode codeFragment)
	{
		super(codeFragment);
		// fitnessIsSet = false;
	}

	public Double getFitness()
	{
		return fitness;
	}
}