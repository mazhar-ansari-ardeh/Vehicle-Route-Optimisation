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
	int counter;

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
		this.counter = 0;
	}

	public void increaseCounter()
	{
		counter++;
	}

	int getCounter()
	{
		return counter;
	}

	@Override
	public int hashCode()
	{
		return codeFragment.hashCode();
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
					+ "," + Integer.toString(counter)
					+ "]";
	}

	@Override
	public String toString()
	{
		return "["
					+ (codeFragment == null ? "null" : codeFragment.makeCTree(false, false, false))
					+ "," + Integer.toString(counter)
					+ "]";
	}
}
