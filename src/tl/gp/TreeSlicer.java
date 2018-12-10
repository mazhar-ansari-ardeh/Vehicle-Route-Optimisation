package tl.gp;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleProblemForm;
import gputils.terminal.DoubleERC;
import gputils.terminal.TerminalERC;
import javafx.util.Pair;

public class TreeSlicer
{
	/**
	 * Extracts trees, in the form of <code>GPIndividuals</code> from a <code>GPIndividual</code>
	 * object. The method extracts immediate children of the root of the <code>ind</code> input
	 * parameter and returns them as new <code>GPIndividual</code> objects. <p>
	 * <b>Note:</b> This method modifies the <code>ind</code> input parameter so that it loses all
	 * its children, even the children that are terminals.
	 * @param ind A <code>GPIndividual</code> to be used to extract trees from.
	 * @param includeTerminals if <code>true</code>, the method will also extract terminals from
	 * <code>ind</code>. Otherwise, immediate terminal children of the root of <code>ind</code> will
	 * be ignored.
	 * @return A list of <GPIndividual> objects that are extracted from the given <code>ind</code>.
	 * The return value is never <code>null</code>.
	 */
	public static ArrayList<GPIndividual> extractToTreesz(GPIndividual ind,
			boolean includeTerminals)
	{
		ArrayList<GPIndividual> retval = new ArrayList<>();
		if(ind == null)
			return retval;

		if(ind.trees[0].child.children == null
				|| ind.trees[0].child.children.length < 2)
			return retval;


		for(int i = 0; i < ind.trees[0].child.children.length; i++)
		{
			GPIndividual x = ind.lightClone();
			x.trees[0].child = x.trees[0].child.children[i];
			if(x.trees[0].child.children.length > 0
					|| (x.trees[0].child.children.length == 0 && includeTerminals))
			{
				x.trees[0].child.parent = x.trees[0];
				retval.add(x);
			}
		}

		ind.trees[0].child.children = new GPNode[0];


		return retval;
	}

	/**
	 * Extracts trees, in the form of <code>GPNode</code> objects from a <code>GPIndividual</code>
	 * object. The method extracts immediate children of the root of the <code>ind</code> input
	 * parameter and returns them as new <code>GPIndividual</code> objects. <p>
	 * <b>Note:</b> This method modifies the <code>ind</code> input parameter so that it loses all
	 * its children, even the children that are terminals.
	 * @param ind A <code>GPIndividual</code> to be used to extract trees from.
	 * @param includeTerminals if <code>true</code>, the method will also extract terminals from
	 * <code>ind</code>. Otherwise, immediate terminal children of the root of <code>ind</code>
	 * will be ignored.
	 * @return A list of <GPIndividual> objects that are extracted from the given <code>ind</code>.
	 * The return value is never <code>null</code>.
	 */
	public static ArrayList<GPNode> sliceRootChildrenToNodes(GPIndividual ind,
			boolean includeTerminals)
	{
		ArrayList<GPNode> retval = new ArrayList<>();
		if(ind == null)
			return retval;

		if(ind.trees[0].child.children == null
				|| ind.trees[0].child.children.length < 1)
			return retval;

		for(int i = 0; i < ind.trees[0].child.children.length; i++)
		{
			GPNode x = ind.trees[0].child.children[i];
			if(x.children.length > 0 || (x.children.length == 0 && includeTerminals))
			{
				x.parent = null;
				retval.add(x);
			}
		}

		ind.trees[0].child.children = new GPNode[0];

		return retval;
	}

	/**
	 * Extracts trees, in the form of <code>GPNode</code> objects from a <code>GPIndividual</code>
	 * object. The method extracts recursively all possible subtrees of the <code>ind</code> input
	 * parameter and returns them.
	 * <b>Note:</b> This method modifies the <code>ind</code> input parameter so that it loses all
	 * its children, even the children that are terminals.
	 * @param root A <code>GPNode</code> to be used to extract subtrees from.
	 * @param includeTerminals if <code>true</code>, the method will also extract terminals
	 * from <code>ind</code>. Otherwise, terminals will be ignored.
	 * @return A list of <GPNode> objects that are extracted from the given <code>ind</code>.
	 * The return value is never <code>null</code>.
	 */
	public static ArrayList<GPNode> sliceAllToNodes(GPNode root, boolean includeTerminals)
	{
		ArrayList<GPNode> retval = new ArrayList<>();
		if(root == null)
			return retval;

		// It does not have any children then it is a terminal
		if(root.children == null || root.children.length == 0)
		{
			if(includeTerminals)
				retval.add((GPNode)root.clone());
			return retval;
		}
		GPNode rootClone = (GPNode)root.clone();
		rootClone.parent = null;
		retval.add(rootClone);
		for(int i = 0; i < root.children.length; i++)
			retval.addAll(sliceAllToNodes(root.children[i], includeTerminals));

		return retval;
	}

	public static ArrayList<GPNode> sliceAllToNodes(GPIndividual ind, boolean includeTerminals)
	{
		ArrayList<GPNode> retval = new ArrayList<>();
		if(ind == null)
			return retval;

		for(GPTree tree : ind.trees)
			retval.addAll(sliceAllToNodes(tree.child, includeTerminals));

		return retval;
	}


	private static void insertConstantERC(GPNode root, String featureName)
	{
		// It does not have any children then it is a terminal
		if(root.children == null || root.children.length == 0)
		{
			if(((TerminalERC)root).getTerminal().name().equals(featureName))
			{
				DoubleERC constNode = new DoubleERC();
				constNode.value = 1;

				GPNode parent = (GPNode)root.parent;
				constNode.parent = parent;
				constNode.argposition = root.argposition;
				parent.children[root.argposition] = constNode;
				root.parent = null;
			}
			return;
		}
		for(GPNode child : root.children)
		{
			insertConstantERC(child, featureName);
		}
	}

	public static Pair<Double, Double> getFeatureContribution(EvolutionState state, GPIndividual ind
			, String featureName)
	{
		ind.evaluated = false;
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, 0, 0);
		double oldFitness = ind.fitness.fitness();

		System.out.println(oldFitness);
 		System.out.println(ind.trees[0].child.makeGraphvizTree());

		GPIndividual gind = (GPIndividual)ind.clone();

		insertConstantERC(gind.trees[0].child, featureName);

		gind.evaluated = false;
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
		double newFitness = gind.fitness.fitness();
		System.out.println(newFitness);
		System.out.println(gind.trees[0].child.makeGraphvizTree());

		return new Pair<>(oldFitness, newFitness);
	}

	private static double getSubtreeContrib(EvolutionState state, GPIndividual theIndividual,
			GPNode theNode)
	{
		// Insert the constant into the original tree
		DoubleERC constNode = new DoubleERC();
		constNode.value = 1;
		constNode.parent = theNode.parent;
		GPNode parent = (GPNode) theNode.parent;
		parent.children[theNode.argposition] = constNode;

		// Evaluate the tree with the inserted constant
		theIndividual.evaluated = false;
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, theIndividual, 0, 0);
		double newFitness = theIndividual.fitness.fitness();

		// Replace back the constant
		theNode.parent = parent;
		parent.children[theNode.argposition] = theNode;
		constNode.parent = null;

		return newFitness;
	}

	public static ArrayList<Pair<GPNode, Double>> sliceAllWithContrib(EvolutionState state,
			GPIndividual theIndividual, GPNode root, boolean includeTerminals)
	{
		ArrayList<Pair<GPNode, Double>> retval = new ArrayList<>();
		if(root == null)
			return retval;

		// It does not have any children then it is a terminal
		if(root.children == null || root.children.length == 0)
		{
			if(includeTerminals)
			{
				double theFitness = getSubtreeContrib(state, theIndividual, root);
				retval.add(new Pair<>((GPNode)root.clone(), theFitness));
			}
			return retval;
		}
		GPNode rootClone = (GPNode)root.clone();
		rootClone.parent = null;

		double newFitness = getSubtreeContrib(state, theIndividual, root);

		retval.add(new Pair<>(rootClone, newFitness));

		for(int i = 0; i < root.children.length; i++)
			retval.addAll(sliceAllWithContrib(state, theIndividual, root.children[i],
					includeTerminals));

		return retval;
	}

	public static ArrayList<Pair<GPNode, Double>> sliceAllWithContrib(EvolutionState state,
											GPIndividual ind, boolean includeTerminals)
	{
		ArrayList<Pair<GPNode, Double>> retval = new ArrayList<>();
		if(ind == null)
			return retval;

		GPIndividual gind = (GPIndividual) ind.clone();
		for(GPTree tree : gind.trees)
			retval.addAll(sliceAllWithContrib(state, gind, tree.child, includeTerminals));

		return retval;
	}
}
