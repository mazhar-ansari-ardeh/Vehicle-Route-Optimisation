package tl.knowledge.codefragment;

import ec.gp.GPNode;
import tl.knowledge.KnowledgeItem;

public class CodeFragmentKI implements KnowledgeItem<GPNode>
{
	GPNode codeFragment;
	int counter;

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
}
