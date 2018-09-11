package tutorial7.knowledge.codefragment.fitted;

import ec.gp.GPNode;
import tutorial7.knowledge.codefragment.CodeFragmentKI;

public class FittedCodeFragment extends CodeFragmentKI
{
	double fitness;

	public FittedCodeFragment(GPNode codeFragment, double fitness)
	{
		super(codeFragment);
		this.fitness = fitness;
	}
}