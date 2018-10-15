package tl.gp;

import java.util.ArrayList;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;

public class TreeSlicer
{
	/**
	 * Extracts trees, in the form of <code>GPIndividuals</code> from a <code>GPIndividual</code> object. The method extracts
	 * immediate children of the root of the <code>ind</code> input parameter and returns them as new <code>GPIndividual</code>
	 * objects. <p>
	 * <b>Note:</b> This method modifies the <code>ind</code> input parameter so that it loses all its children, even the
	 * children that are terminals.
	 * @param ind A <code>GPIndividual</code> to be used to extract trees from.
	 * @param includeTerminals if <code>true</code>, the method will also extract terminals from <code>ind</code>. Otherwise,
	 * immediate terminal children of the root of <code>ind</code> will be ignored.
	 * @return A list of <GPIndividual> objects that are extracted from the given <code>ind</code>. The return value is never
	 * <code>null</code>.
	 */
	public static ArrayList<GPIndividual> extractToTrees(GPIndividual ind, boolean includeTerminals)
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
			if(x.trees[0].child.children.length > 0 || (x.trees[0].child.children.length == 0 && includeTerminals))
			{
				x.trees[0].child.parent = x.trees[0];
				retval.add(x);
			}
		}

		ind.trees[0].child.children = new GPNode[0];


		return retval;
	}

	/**
	 * Extracts trees, in the form of <code>GPNode</code> objects from a <code>GPIndividual</code> object. The method extracts
	 * immediate children of the root of the <code>ind</code> input parameter and returns them as new <code>GPIndividual</code>
	 * objects. <p>
	 * <b>Note:</b> This method modifies the <code>ind</code> input parameter so that it loses all its children, even the
	 * children that are terminals.
	 * @param ind A <code>GPIndividual</code> to be used to extract trees from.
	 * @param includeTerminals if <code>true</code>, the method will also extract terminals from <code>ind</code>. Otherwise,
	 * immediate terminal children of the root of <code>ind</code> will be ignored.
	 * @return A list of <GPIndividual> objects that are extracted from the given <code>ind</code>. The return value is never
	 * <code>null</code>.
	 */
	public static ArrayList<GPNode> sliceRootChildrenToNodes(GPIndividual ind, boolean includeTerminals)
	{
		ArrayList<GPNode> retval = new ArrayList<>();
		if(ind == null)
			return retval;

		if(ind.trees[0].child.children == null
				|| ind.trees[0].child.children.length < 2)
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

	public static ArrayList<GPNode> sliceAllToNodes(GPNode root, boolean includeTerminals)
	{
		ArrayList<GPNode> retval = new ArrayList<>();
		if(root == null)
			return retval;

		if(root.children == null || root.children.length == 0) // It does not have any children then it is a terminal
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
}
