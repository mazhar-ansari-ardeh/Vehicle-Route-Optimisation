package tl.gp;

public enum KnowledgeExtractionMethod
{
	/**
	 * The knowledge source contains exact code fragments so that loaded code fragments do not need 
	 * to be processed after loading. 
	 */
	ExactCodeFragment, 
	/**
	 * All possible subtrees of a tree will be extracted
	 */
	AllSubtrees,

	/**
	 * Only subtrees of root will be sliced out.
	 */
	RootSubtree,
	
	/**
	 * Only consider the root of individual. As a matter of fact, this method considers the whole
	 * individual as a code fragment.
	 */
	Root;
	
	public static KnowledgeExtractionMethod parse(String method)
	{
		if(method.equals("exactcodefragment"))
			return ExactCodeFragment;
		if(method.equals("all"))
			return KnowledgeExtractionMethod.AllSubtrees;
		else if(method.equals("rootsubtree"))
			return KnowledgeExtractionMethod.RootSubtree;
		else if(method.equals("root"))
			return KnowledgeExtractionMethod.Root;
		else
		{
			return null;
		}
	}
}