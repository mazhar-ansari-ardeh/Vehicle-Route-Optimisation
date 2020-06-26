package tl.gp;

import java.util.*;

import ec.Fitness;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPNodeParent;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.gp.terminal.feature.Fullness;
import gphhucarp.gp.terminal.feature.RemainingCapacity;
import gphhucarp.gp.terminal.feature.ServeCost;
import gputils.function.Add;
import gputils.function.Div;
import gputils.function.Mul;
import gputils.function.Sub;
import gputils.terminal.TerminalERCUniform;
import org.apache.commons.lang3.tuple.Pair;
//import tl.collections.tree.TreeNode;

public class GPIndividualUtils
{
	public static TLGPIndividual asGPIndividual(GPNode root)
	{
		if(root == null)
			throw new IllegalArgumentException("Root node cannot be null.");

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
	 * Creates a new {@code TLGPIndividual} object based on the given tree whose root node is the input parameter
	 * {@code root}. The method sets the returned object as unevaluated.
	 * @param root the root node of a tree.
	 * @param fitness the fitness object to use as the fitness of the created object.
	 * @return a new {@code TLGPIndividual} that has one tree whose root node and fitness objects are given.
	 */
	public static TLGPIndividual asGPIndividual(GPNode root, Fitness fitness)
	{
		TLGPIndividual retval = asGPIndividual(root);
		retval.fitness = fitness;
		retval.evaluated = false;
		return retval;
	}

	public static TLGPIndividual asGPIndividual(GPNode root, double fitness)
	{
		TLGPIndividual retval = asGPIndividual(root);
		MultiObjectiveFitness aFitness = new MultiObjectiveFitness();
		aFitness.objectives = new double[]{fitness};
		aFitness.maximize = new boolean[]{false};
		retval.fitness = aFitness;
		retval.evaluated = false;
		return retval;
	}

	/**
	 * Gets a {@code GPNode} object and creates a {@code GPTree} object that contains that. The method severs any connection
	 * that the given node may have to its parent.
	 * @param node A node that should be contained in a tree. The value of this parameter can be {@code null} in which case
	 *             the returned tree will have {@code null} as its child.
	 * @return A {@code GPTree} object that contains the given node.
	 */
	public static GPTree asGPTree(GPNode node)
	{
		GPTree tree = new GPTree();
		tree.child = node;

		if(node == null)
			return tree;

		GPNodeParent parent = node.parent;
		if(parent != null)
		{
			if(parent instanceof GPNode)
			{
				GPNode p = (GPNode)parent;
				p.children[node.argposition] = null;
			}
			else if(parent instanceof GPTree)
			{
				GPTree t = (GPTree) parent;
				t.child = null;
			}
		}

		node.parent = tree;
		node.argposition = 0;

		return tree;
	}

	/**
	 * The function strips the root node of the trees of the given individual out of the individual. This function
	 * modifies the given individual so that it cuts the link between the individual and its root.
	 * @param ind a {@code GPIndividual} object to get strip roots from.
	 * @return A list of roots of the given individual. The returned value will be an empty list if the trees of the given
	 * individual do not contain any roots.
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
			retval.add(node);
		}
		return retval;
	}

	/**
	 * Inserts a given GP node (subtree) in place of a subtree inside individual.
	 * @param at The subtree of that will be cut from its place to be replaced with the subtree given by <code> with</code>.
	 *           The function cuts the parent link of this parameter and replaces it with <code>null</code>. This parameter
	 *           cannot be <code>null</code>.
	 * @param with The subtree that will be inserted in place of <code>at</code>. This parameter cannot be <code>null</code>.
	 */
	public static void replace(GPNode at, GPNode with)
	{
		if(at == null || with == null)
			throw new IllegalArgumentException("Null cannot be used here");

		if(at.parent instanceof GPNode)
		{
			GPNode parent = (GPNode)at.parent;
			with.parent = parent;
			with.argposition = at.argposition;
			parent.children[at.argposition] = with;
			at.parent = null;
		}
		else if (at.parent instanceof GPTree)
		{
			GPTree parent = (GPTree)at.parent;
			with.parent = parent;
			with.argposition = at.argposition;
			parent.child = with;
			at.parent = null;
		}
	}

	/**
	 * Receives a node object as parent and also a set of node children and adds the set of children to the children of
	 * the given parent. The method throws an exception if the parent object already has at least one child.
	 *
	 * @param parent The parent node. This argument cannot be {@code null}.
	 * @param createChildren if {@code true}, the method will assume that the given parent does not already have
	 *                       children and will create new array of children and then copies the given children inside.
	 *                       If this parameter is {@code false}, then the method assumes that the given parent does not
	 *                       have an array of children and will first create one for it.
	 *                       <p>This argument is needed because some GP functions create their array of children
	 *                       automatically during their construction which can be reused here. </p>
	 * @param children The set of children. This argument cannot be {@code null}.
	 */
	public static void addChildrenTo(GPNode parent, boolean createChildren, GPNode... children)
	{
		if(children == null)
			throw new IllegalArgumentException("Children cannot be null");
//		if(!createChildren && parent.children != null && parent.children.length > 0)
//			throw new IllegalArgumentException("The given parent already has children");

		if(createChildren)
		{
			if(parent.children != null && parent.children.length != 0)
				throw new IllegalArgumentException("Parent already has children");

			parent.children = new GPNode[children.length];
		}
		else
		{
			if(parent.children == null || parent.children.length != children.length)
			{
				throw new IllegalArgumentException("Parent is supposed to have" + children.length
													+ " children but it does not.");
			}
		}
		for(int i = 0; i < children.length; i++)
		{
			parent.children[i] = children[i];
			children[i].argposition = (byte) i;
			children[i].parent = parent;
		}
	}

	/**
	 * Index the nodes of a tree whose root is given as input. For the case of this method, the index of a node is
	 * actually a string that can be interpreted as the address of the node inside the tree. The address is calculated
	 * as follows:
	 * <p> - The address of the root node is "-1",
	 * <p> - The address of each node is the address of its parent that is appended with the position of the node
	 * amongst its parent's children.
	 *
	 * @param root the root node of the tree to be indexed. This parameter cannot be {@code null}.
	 * @return A dictionary that maps an address to a node. This method does not clone or tear out the nodes from their
	 * trees and as a result, the connections that each node may have with other nodes inside the tree will remain
	 * intact.
	 */
	public static Map<String, GPNode> index(GPNode root)
	{
		Map<String, GPNode> indice  = new HashMap<>();
		indice.put("-1", root);
		index(root, "-1", indice);

//		indice.sort(Comparator.naturalOrder());

		return indice;
	}

	/*
	 * Creates the index of the nodes recursively.<br/>
	 *
	 * @see #index(GPNode, String, Map)
	 * @param node current node to work with.
	 * @param address address of {@code node}.
	 * @param indice the index list that the method updates recursively.
	 */
	private static void index(GPNode node, String address, Map<String, GPNode> indice)
	{
		for(int i = 0; i < node.children.length; i++)
		{
			String chAddress = (address.equals("-1")? "" : address) + i;
			indice.put(chAddress, node.children[i]);
			index(node.children[i], chAddress, indice);
		}
	}

	static public GPIndividual sampleInd()
	{
		TerminalERCUniform scn1 = new TerminalERCUniform();
		ServeCost sc = new ServeCost();
		scn1.children = new GPNode[0];
		scn1.setTerminal(	sc);

		TerminalERCUniform scn2 = new TerminalERCUniform();
		ServeCost sc2 = new ServeCost();
		scn2.children = new GPNode[0];
		scn2.setTerminal(sc2);

//		TerminalERCUniform rqn1 = new TerminalERCUniform();
//		RemainingCapacity rc = new RemainingCapacity();
//		rqn1.children = new GPNode[0];
//		rqn1.setTerminal(rc);

		TerminalERCUniform rqn2 = new TerminalERCUniform();
		RemainingCapacity rc2 = new RemainingCapacity();
		rqn2.children = new GPNode[0];
		rqn2.setTerminal(rc2);

		TerminalERCUniform fuln1 = new TerminalERCUniform();
		Fullness ful1 = new Fullness();
		fuln1.children = new GPNode[0];
		fuln1.setTerminal(ful1);

		TerminalERCUniform fuln2 = new TerminalERCUniform();
		Fullness ful2 = new Fullness();
		fuln2.children = new GPNode[0];
		fuln2.setTerminal(ful2);

		GPNode div1 = new Div();
		addChildrenTo(div1, false, ful1, ful2);

		GPNode plus1 = new Add();
		addChildrenTo(plus1, false, scn1, scn2);
		GPNode mul1 = new Mul();
		addChildrenTo(mul1, false, div1, rqn2);

		GPNode min = new Sub();
		addChildrenTo(min, false, plus1, mul1);

//		GPTree tree = new GPTree();
//		tree.child = min;
//		min.parent = tree;
//		min.argposition = 0;

		return asGPIndividual(min);
	}


	/**
	 * Returns an iterator for the tree whose root node is given as the input parameter. The returned iterator traverses
	 * the given tree in an infix order.
	 * @param root The root node of the tree that should be iterated. This parameter can be empty in which case the
	 *             returned iterator will not perform any iteration.
	 * @return An instance of {@code Iterator<GPNode>} that can iterate the given tree in an infix order.
	 */
	public static Iterator<GPNode> iterator(GPNode root)
	{
		class PreFixTreeIterator implements Iterator<GPNode>
		{
			GPNode cursor;
			Stack<Pair<GPNode, Integer>> stack = new Stack<>();

			public PreFixTreeIterator(GPNode root)
			{
				this.cursor = root;
			}

			@Override
			public boolean hasNext()
			{
				return cursor != null;
			}

			@Override
			public GPNode next()
			{
				if(cursor == null)
					return null;

				GPNode retval = cursor;
				Pair<GPNode, Integer> node = Pair.of(cursor, 0);
				if(node.getValue() < node.getKey().children.length)
				{
					cursor = node.getKey().children[node.getValue()];
					node = Pair.of(node.getKey(), node.getValue()+1);
					stack.push(node);
				}
				else
				{
					node = stack.pop();
					// Are the children of this node have been iterated? Then discard them.
					while(!(node.getValue() < node.getKey().children.length)
							&& !stack.isEmpty())
					{
						node = stack.pop();
					}
					// Finished iterating the tree?
					if(stack.isEmpty() && !(node.getValue() < node.getKey().children.length))
					{
						cursor = null;
						return retval;
					}
					cursor = node.getKey().children[node.getValue()];
					node = Pair.of(node.getKey(), node.getValue() + 1);
					stack.push(node);
				}
				return retval;
			}
		}

		return new PreFixTreeIterator(root);
	}

	public static void main(String[] args) {
//		GPIndividual ind = sampleInd();
//		int depth = ind.trees[0].child.depth();
//		System.out.println(depth);
//		Iterator<GPNode> it = iterator(ind.trees[0].child);
//		while(it.hasNext())
//		{
//			GPNode next = it.next();
//			System.out.println(next);
//		}
	}
}
