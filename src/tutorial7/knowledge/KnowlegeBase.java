package tutorial7.knowledge;


/*
 * Design considerations, goals, hopes and ideals: 
 * 	* It should be able receive knowledge item to store
 * 	* It should be able to handle co-evolution 
 * 	* It should be able to handle memory schemes 
 */
public interface KnowlegeBase
{
	// TODO: Design method input argument.
	void addItem();

	// TODO: The signature of this method is not complete.
	void removeItem();

	// TODO: This method is not complete.
	boolean contains();

	// The iterator for this collection
	public KnowledgeExtractor getKnowledgeExtractor();
}
