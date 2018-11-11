package tl.knowledge.codefragment;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import ec.util.MersenneTwisterFast;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.KnowledgeItem;

public class RandomKnowledgeExtractor<T> implements KnowledgeExtractor
{
	private Map<Integer, ? extends KnowledgeItem<T>> repository;
	private MersenneTwisterFast random;
	
	public RandomKnowledgeExtractor(Map<Integer, ? extends KnowledgeItem<T>> repository, 
			MersenneTwisterFast random)
	{
		this.repository = repository;
		this.random = random;
	}

	@Override
	public boolean hasNext()
	{
		return !repository.isEmpty();
	}

	@Override
	public KnowledgeItem<T> getNext()
	{
		if(repository.isEmpty())
			return null;
		ArrayList<Entry<Integer, ? extends KnowledgeItem<T>>> entries = new ArrayList<>(repository.entrySet());
		return entries.get(random.nextInt(entries.size())).getValue();
	}

	@Override
	public void reset()
	{
		 // No need to do anything.
	}
}