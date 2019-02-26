package tl.gp;

import java.util.ArrayList;

import ec.gp.GPIndividual;
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

	/**
	 * The function strips the root of the given individual out of the individual. This function
	 * modifies the given individual so that it cuts the link between the individual and its root.
	 * @param ind a {@code GPIndividual} object to get strip roots from.
	 * @return A list of roots of the given individual.
	 */
	public static ArrayList<GPNode> stripRoots(GPIndividual ind)
	{
		if(ind == null || ind.trees == null)
			throw new IllegalArgumentException("Individual or its subtrees cannot be null");

		ArrayList<GPNode> retval = new ArrayList<>();

		for(int i = 0; i < ind.trees.length; i++)
		{
			GPNode node = ind.trees[i].child;
			ind.trees[i].child = null;
			node.parent = null;
		}
		return retval;
	}
}
