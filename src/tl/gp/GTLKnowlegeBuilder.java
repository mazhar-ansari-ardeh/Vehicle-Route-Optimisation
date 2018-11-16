package tl.gp;

import java.nio.file.*;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.TLLogger;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.GTLCodeFragmentKB;

public class GTLKnowlegeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

// 	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_KNOWLEDGE_FOLDER = "knowledge-folder";

	private static KnowledgeExtractor extractor = null;

	private int knowledgeSuccessLogID;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter knowledgeFolderParam = base.push(P_KNOWLEDGE_FOLDER);
		String knowledgeFolder = state.parameters.getString(knowledgeFolderParam, null);

		GTLCodeFragmentKB knowledgeBase = new GTLCodeFragmentKB();
		knowledgeBase.extractFrom(Paths.get(knowledgeFolder), ".bin");
		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("TLGKnowlegeBuilder loaded.");

		knowledgeSuccessLogID = setupLogger(state, base);
	}


	/**
	 * Number of times that code fragments have been transferred.
	 */
	private int transferCount = 0;

	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
			final GPNodeParent parent, final GPFunctionSet set,	final int argposition,
			final int requestedSize)
	{
		int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		int numToTransfer = Math.round(0.3f * popSize);
		if(transferCount < numToTransfer)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				log(state, cf, ++transferCount, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				return node;
			}
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
	}
}
