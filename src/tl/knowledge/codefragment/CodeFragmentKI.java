package tl.knowledge.codefragment;

import java.io.Serializable;

import ec.gp.GPNode;
import tl.knowledge.KnowledgeItem;

public class CodeFragmentKI implements KnowledgeItem<GPNode>, Serializable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	GPNode codeFragment;
	int useCounter;
	
	private int duplicateCount = 0;

	protected CodeFragmentKI()
	{
//		codeFragment = null;
//		counter = 0;
	}

	public CodeFragmentKI(GPNode codeFragment)
	{
		if (codeFragment == null)
		{
			throw new NullPointerException("Code fragment cannot be null");
		}

		this.codeFragment = codeFragment;
		this.useCounter = 0;
	}
	
	public void incrementDuplicateCount()
	{
		duplicateCount++;
	}
	
	public int getDuplicateCount()
	{
		return duplicateCount;
	}

	public void incrementCounter()
	{
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

	public String toDotString()
	{
		return "["
					+ (codeFragment == null ? "null" : codeFragment.makeGraphvizTree())
					+ "," + Integer.toString(useCounter) + ", " + Integer.toString(duplicateCount)
					+ "]";
	}

	@Override
	public String toString()
	{
		return "["
					+ (codeFragment == null ? "null" : codeFragment.makeCTree(false, true, true))
					+ "," + Integer.toString(useCounter) + ", " + Integer.toString(duplicateCount)
					+ "]";
	}
}
