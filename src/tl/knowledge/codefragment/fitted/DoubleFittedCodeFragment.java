package tl.knowledge.codefragment.fitted;

import java.io.Serializable;

import ec.Fitness;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import tl.knowledge.codefragment.CodeFragmentKI;

public class DoubleFittedCodeFragment extends CodeFragmentKI implements Serializable
{
	private static final long serialVersionUID = 1L;
	Double fitnessOnSource = null;
	Double fitnessOnTarget = null;
	
//	public DoubleFittedCodeFragment()
//	{
//		super();
//	}

	public DoubleFittedCodeFragment(GPNode codeFragment, Double fitnessOnTarget, 
									Double fitnessOnSource)
	{
		super(codeFragment);
		this.fitnessOnTarget = fitnessOnTarget;
		this.fitnessOnSource = fitnessOnSource;
	}

	public DoubleFittedCodeFragment(GPNode codeFragment)
	{
		super(codeFragment);
	}
	
	public Double getFitnessOnSource()
	{
		return fitnessOnSource;
	}
	
	public Double getFitnessOnTarget()
	{
		return fitnessOnTarget;
	}
	
	public void setFitnessOnTarget(double fitness)
	{
		this.fitnessOnTarget = fitness;
	}
	
	public String toString()
	{
		String retval = getItem().makeGraphvizTree().replaceAll("\n", "");
		retval += ", \t" + fitnessOnSource + ", \t" + fitnessOnTarget;
		
		return retval;
	}
	
	/**
	 * Converts this code fragment into a GP individual. This method makes a deep clone of the code
	 * fragment that it stores so that any modifications to the returned value will not impact the 
	 * code fragment stored here. <p>
	 * <b>Note</b>: the fitness value of the returned object will NOT be set and therefore, any 
	 * fitness value that may already be stored in {@code this} object will not be transfered to 
	 * the returned value.
	 * @param fitnessPrototype The fitness object that will be used as the prototype for creating 
	 * the fitness of the returned code fragment.
	 * @return a new {@code GPIndividual} object that contains this code fragment.
	 */
	public GPIndividual asIndividual(Fitness fitnessPrototype)
	{
		GPIndividual ret = new GPIndividual();
		ret.evaluated = false;
		ret.fitness = (Fitness) fitnessPrototype.clone();
		ret.trees = new GPTree[1];
		ret.trees[0] = new GPTree();
		ret.trees[0].owner = ret;
		GPNode item = (GPNode) getItem().clone();
		ret.trees[0].child = item;
		item.parent = ret.trees[0].child;
		item.argposition = 0;
		
		return ret;
	}
}