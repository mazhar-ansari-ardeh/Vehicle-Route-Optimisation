package tutorial7.knowledge.codefragment;

import ec.gp.GPNode;
import tutorial7.knowledge.KnowledgeItem;

public class CodeFragmentKI implements KnowledgeItem
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
}
