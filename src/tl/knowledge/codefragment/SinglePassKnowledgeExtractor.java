package tl.knowledge.codefragment;

import java.util.Iterator;
import java.util.Map;

import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.KnowledgeItem;

public class SinglePassKnowledgeExtractor<T> implements KnowledgeExtractor
{

	private Iterator<Integer> iter;

	// integer is hashcode
	private Map<Integer, ? extends KnowledgeItem<T>> repository;

	public SinglePassKnowledgeExtractor(Map<Integer, ? extends KnowledgeItem<T>> repository)
	{
		 iter = repository.keySet().iterator();
		 this.repository = repository;
	}

	@Override
	public boolean hasNext()
	{
		return iter.hasNext();
	}

	@Override
	public KnowledgeItem<T> getNext()
	{
		if(iter.hasNext())
		{
			KnowledgeItem<T> retval = repository.get(iter.next());
			retval.incrementCounter();
			return retval;
		}
		else
			return null;
	}

	@Override
	public void reset()
	{
		 iter = repository.keySet().iterator();
	}

}
