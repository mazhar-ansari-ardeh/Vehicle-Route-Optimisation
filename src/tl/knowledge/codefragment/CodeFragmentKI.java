package tl.knowledge.codefragment;

import java.io.Serializable;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import tl.gp.GPIndividualUtils;
import tl.knowledge.KnowledgeItem;

public class CodeFragmentKI implements KnowledgeItem<GPNode>, Serializable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private GPNode codeFragment;

	/**
	 * Number of times that this item has been used. A value of zero means that the item was not
	 * used at all.
	 */
	private int useCounter = 0;

	/**
	 * Number of times that a duplicate of this item was found in source domain.
	 * A value of zero for this field means that the item did not have any duplicate in the source
	 * domain.
	 */
	private int duplicateCount = 0;

//	/**
//	 * If a sourceGene
//	 */
//	public final String FINAL_SOURCE_GENERATION = "final-gen";

	/**
	 * The generation of the source domain that this code fragment was extracted from.
	 * An empty value means that this field is not set.
	 */
	private String origin = "";

//	protected CodeFragmentKI()
//	{
////		codeFragment = null;
////		counter = 0;
//	}

	public CodeFragmentKI(GPNode codeFragment)
	{
		if (codeFragment == null)
		{
			throw new NullPointerException("Code fragment cannot be null");
		}

		this.codeFragment = codeFragment;
	}

	public CodeFragmentKI(GPNode codeFragment, String origin)
	{
		this(codeFragment);
		this.origin = origin;
	}

	public void incrementDuplicateCount()
	{
//		if(duplicateCount < 0)
//			duplicateCount = 0;

		duplicateCount++;
	}

	public int getDuplicateCount()
	{
		return duplicateCount;
	}

	@Override
	public void incrementCounter()
	{
//		if(useCounter < 0)
//			useCounter = 0;

		useCounter++;
	}

	int getCounter()
	{
		return useCounter;
	}

	@Override
	public int hashCode()
	{
		return codeFragment.rootedTreeHashCode();
	}

	@Override
	public GPNode getItem()
	{
		return codeFragment;
	}

	public GPIndividual getAsIndividual()
	{
		return GPIndividualUtils.asGPIndividual(codeFragment);
//		GPIndividual retval = new GPIndividual();
//		retval.trees = new GPTree[1];
//		retval.trees[0] = new GPTree();
//		retval.trees[0].child = codeFragment;
//		codeFragment.parent =retval.trees[0];
//
//		MultiObjectiveFitness fitness = new MultiObjectiveFitness();
//		fitness.objectives = new double[1];
//
//		retval.fitness = fitness;
//		retval.evaluated = false;
//
//		return retval;
	}

	public String getOrigin()
	{
		return origin;
	}

	public String toDotString()
	{
		return "["
					+ (codeFragment == null ? "null" : codeFragment.makeGraphvizTree())
					+ ", use: " + Integer.toString(useCounter) + ", dup: "
					+ Integer.toString(duplicateCount) + ", o: " + origin + "]";
	}

	@Override
	public String toString()
	{
		return "["
					+ (codeFragment == null ? "null" : codeFragment.makeCTree(false, true, true))
					+ ", use:" + Integer.toString(useCounter) + ", dup: "
					+ Integer.toString(duplicateCount) + ", o:" + origin + "]";
	}
}
