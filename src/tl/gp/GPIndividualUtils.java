package tl.gp;

import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;

public class GPIndividualUtils
{
	public static TLGPIndividual asGPIndividual(GPNode root)
	{
		TLGPIndividual retval = new TLGPIndividual();
		retval.trees = new GPTree[1];
		retval.trees[0] = new GPTree();
		retval.trees[0].child = root;
		root.parent =retval.trees[0];

		MultiObjectiveFitness fitness = new MultiObjectiveFitness();
		fitness.objectives = new double[1];

		retval.fitness = fitness;
		retval.evaluated = false;

		return retval;
	}
}
