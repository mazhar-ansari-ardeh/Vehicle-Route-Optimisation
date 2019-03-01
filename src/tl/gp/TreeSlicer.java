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
				if(root.parent instanceof GPNode)
				{
					GPNode parent = (GPNode)root.parent;
					constNode.parent = parent;
					constNode.argposition = root.argposition;
					parent.children[root.argposition] = constNode;
					root.parent = null;
				}
				else if (root.parent instanceof GPTree)
				{
					GPTree parent = (GPTree)root.parent;
					constNode.parent = parent;
					constNode.argposition = root.argposition;
					parent.child = constNode;
					root.parent = null;
				}
			}
			return;
		}
		for(GPNode child : root.children)
		{
			insertConstantERC(child, featureName);
		}
	}

	/**
	 * Calculates the contribution of a feature to an individual's fitness.
	 * @param state The EvolutionState object.
	 * @param ind The individual that the effect of terminal on its contribution is desired.
	 * @param featureName The name of feature to get its contribution.
	 * @param evaluateIndividual if true, the individual will be evaluated and otherwise, it will be
	 * assumed that it has been evaluated before being passed into this method.
	 * @return Contribution as a pair in which the first is the individual's fitness and the new is
	 * the fitness after feature removal.
	 */
	public static Pair<Double, Double> getFeatureContribution(EvolutionState state, GPIndividual ind
			, String featureName, boolean evaluateIndividual)
	{
		if(evaluateIndividual)
		{
			ind.evaluated = false;
			((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, 0, 0);
		}
		double oldFitness = ind.fitness.fitness();

//		System.out.println(oldFitness);
// 		System.out.println(ind.trees[0].child.makeGraphvizTree());

		GPIndividual gind = (GPIndividual)ind.clone();

		insertConstantERC(gind.trees[0].child, featureName);

		gind.evaluated = false;
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
		double newFitness = gind.fitness.fitness();
// 		System.out.println(newFitness + "\n");
//		System.out.println(gind.trees[0].child.makeGraphvizTree());

		return new Pair<>(oldFitness, newFitness);
	}


	/**
	 * Calculates the contribution of a feature to an individual's fitness.
	 * @param state The EvolutionState object.
	 * @param theIndividual The individual that the contribution of subtree on its contribution is desired.
	 * @param theNode The root of subtree to get its contribution.
	 * @return Contribution of subtree to individual's fitness. This is measured as
	 * 		   (fitnessWithSubtree - fitnessWithoutSubtree).
	 */
	private static double getSubtreeContrib(EvolutionState state, GPIndividual theIndividual,
			GPNode theNode)
	{
		double oldFitness = theIndividual.fitness.fitness();

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

		return oldFitness - newFitness;
	}

	/**
	 * extracts root subtrees from an individual and also calculates its contribution to it.
	 *
	 * @param state The <code>EvolutionState</code> object
	 * @param theIndividual the individual to slice.
	 * @param minDepth minimum allowed size of subtrees to consider. Size of a subtree is less than (but not equal to) this,
	 *                 it will be ignored.
	 * @param maxDepth maximum allowed size of subtrees to consider. Size of a subtree is greater than (but not equal to)
	 *                 this, it will be ignored.
	 * @return A list of extracted subtrees paired with their contribution.
	 */
	public static ArrayList<Pair<GPNode, Double>> sliceRootSubTreesWithContrib(EvolutionState state,
			GPIndividual theIndividual, int minDepth, int maxDepth)
	{
		if(theIndividual instanceof TLGPIndividual)
		{
			if(!((TLGPIndividual) theIndividual).isTested())
			{
				state.output.fatal("GPIndividual is not evaluated on test scenario");
			}
		}
		else
			state.output.warning("GPIndividual is not of type TLGPIndividual");

		ArrayList<Pair<GPNode, Double>> retval = new ArrayList<>();

		for(GPTree tree : theIndividual.trees)
		{
			for(GPNode node: tree.child.children)
			{
				int depth = node.depth();
				if(depth < minDepth || depth > maxDepth)
					continue;

				double contrib = getSubtreeContrib(state, theIndividual, node);
				GPNode nodeClone = (GPNode) node.clone();
				nodeClone.parent = null;
				retval.add(new Pair<GPNode, Double>(nodeClone, contrib));
			}
		}

		return retval;
	}

	/**
	 * extracts all possible subtrees from an individual and also calculates its contribution to it.
	 *
	 * @param state The <code>EvolutionState</code> object
	 * @param theIndividual the individual to slice.
	 * @param root the root of the tree in
	 * @param includeTerminals if true, the function will also consider terminals as subtrees and extract them too. In this
	 *                         case, the min and max constraints are not applied to terminals.
	 * @param minDepth minimum allowed size of subtrees to consider. Size of a subtree is less than (but not equal to) this,
	 *                 it will be ignored.
	 * @param maxDepth maximum allowed size of subtrees to consider. Size of a subtree is greater than (but not equal to)
	 *                 this, it will be ignored.
	 * @return A list of extracted subtrees paired with their contribution.
	 */
	private static ArrayList<Pair<GPNode, Double>> sliceAllWithContrib(EvolutionState state, GPIndividual theIndividual,
																	   GPNode root, boolean includeTerminals, int minDepth,
																	   int maxDepth)
	{
		if(theIndividual instanceof TLGPIndividual)
		{
			if(!((TLGPIndividual) theIndividual).isTested())
			{
				state.output.fatal("GPIndividual is not evaluated on test scenario");
			}
		}
		else
			state.output.warning("GPIndividual is not of type TLGPIndividual");

		ArrayList<Pair<GPNode, Double>> retval = new ArrayList<>();
		if(root == null)
			return retval;

		// It does not have any children then it is a terminal
		if(root.children == null || root.children.length == 0)
		{
			if(includeTerminals)
			{
				double contrib = getSubtreeContrib(state, theIndividual, root);
				retval.add(new Pair<>((GPNode)root.clone(), contrib));
			}
			return retval;
		}

		int depth = root.depth();
		if(!(depth < minDepth || depth > maxDepth) )
		{
			GPNode rootClone = (GPNode) root.clone();
			rootClone.parent = null;

			double contrib = getSubtreeContrib(state, theIndividual, root);
			retval.add(new Pair<>(rootClone, contrib));
		}
		for(int i = 0; i < root.children.length; i++)
			retval.addAll(sliceAllWithContrib(state, theIndividual, root.children[i],
					includeTerminals, minDepth, maxDepth));

		return retval;
	}

	/**
	 * extracts all possible subtrees from an individual and also calculates its contribution to it.
	 *
	 * @param state The <code>EvolutionState</code> object
	 * @param theIndividual the individual to slice.
	 * @param includeTerminals if true, the function will also consider terminals as subtrees and extract them too. In this
	 *                         case, the min and max constraints are not applied to terminals.
	 * @param minDepth minimum allowed size of subtrees to consider. Size of a subtree is less than (but not equal to) this,
	 *                 it will be ignored.
	 * @param maxDepth maximum allowed size of subtrees to consider. Size of a subtree is greater than (but not equal to)
	 *                 this, it will be ignored.
	 * @return A list of extracted subtrees paired with their contribution.
	 */
	public static ArrayList<Pair<GPNode, Double>> sliceAllWithContrib(EvolutionState state,
											GPIndividual theIndividual, boolean includeTerminals, int minDepth, int maxDepth)
	{
		if(theIndividual instanceof TLGPIndividual)
		{
			if(!((TLGPIndividual) theIndividual).isTested())
			{
				state.output.fatal("GPIndividual is not evaluated on test scenario");
			}
		}
		else
			state.output.warning("GPIndividual is not of type TLGPIndividual");

		ArrayList<Pair<GPNode, Double>> retval = new ArrayList<>();
		if(theIndividual == null)
			return retval;

		GPIndividual gind = (GPIndividual) theIndividual.clone();
		for(GPTree tree : gind.trees)
			retval.addAll(sliceAllWithContrib(state, gind, tree.child, includeTerminals, minDepth, maxDepth));

		return retval;
	}
}
