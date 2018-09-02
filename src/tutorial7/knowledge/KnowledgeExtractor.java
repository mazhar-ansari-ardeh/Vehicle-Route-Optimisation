package tutorial7.knowledge;

public interface KnowledgeExtractor
{
	boolean hasNext();

	// TODO: Design the return type.
	// Maybe it is not a bad idea that the class be an instance of Iterator
	void getNext();
}
