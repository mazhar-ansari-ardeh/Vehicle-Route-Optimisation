package tutorial7.knowledge;

public interface KnowledgeExtractor
{
	boolean hasNext();

	// Maybe it is not a bad idea that the class be an instance of Iterator
	KnowledgeItem getNext();

	void reset();
}
