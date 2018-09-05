package tutorial7.knowledge;


/*
 * Design considerations, goals, hopes and ideals:
 * 	* It should be able receive knowledge item to store
 * 	* It should be able to handle co-evolution
 * 	* It should be able to handle memory schemes
 * 	* It should be able to be dynamic
 */
public interface KnowlegeBase
{
	boolean addItem(KnowledgeItem item);

	boolean removeItem(KnowledgeItem item);

	boolean contains(KnowledgeItem item);

	// The iterator for this collection
	public KnowledgeExtractor getKnowledgeExtractor();

	// TODO: I am not sure about this method. Maybe, it is better to use a method like 'count'
	// or 'size' and use their returned value for determine if knowledge base is empty.
	public default boolean isEmpty()
	{
		return true;
	}
}
