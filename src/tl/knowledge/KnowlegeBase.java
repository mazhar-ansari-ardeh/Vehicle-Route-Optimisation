package tl.knowledge;


/*
 * Design considerations, goals, hopes and ideals:
 * 	* It should be able receive knowledge item to store
 * 	* It should be able to handle co-evolution
 * 	* It should be able to handle memory schemes
 * 	* It should be able to be dynamic
 */
public interface KnowlegeBase<T>
{
	boolean addItem(KnowledgeItem<T> item);

	boolean removeItem(KnowledgeItem<T> item);

	boolean contains(KnowledgeItem<T> item);

	// The iterator for this collection
	public KnowledgeExtractor getKnowledgeExtractor();

	// TODO: I am not sure about this method. Maybe, it is better to use a method like 'count'
	// or 'size' and use their returned value for determine if knowledge base is empty.
	public default boolean isEmpty()
	{
		return true;
	}
}
