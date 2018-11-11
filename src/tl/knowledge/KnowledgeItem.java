package tl.knowledge;

public interface KnowledgeItem<T>
{

	T getItem();

	/**
	 * If a knowledge item is successfully used, this method should be invoked to keep track of the
	 * number of times that the item has been used.
	 */
	void incrementCounter();
}
