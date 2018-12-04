package tl.gp;

import java.io.File;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.TLLogger;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.MySimpleCodeFragmentKB;

public class MySimpleCodeFragmentBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	public static final String P_KNOWLEDGE_EXTRACTION = "knowledge-extraction";

	public static final String P_TRANSFER_PERCENT = "transfer-percent";


	private static KnowledgeExtractor extractor = null;

	private int knowledgeSuccessLogID;

	/**
	 * Percentage of the source domain knowledge to be transferred. In this implementation, this
	 * percentage is used both on source and target domains so that for example, a value of .75
	 * means that 75% of individuals in source domain will be considered for knowledge extraction
	 * and also, at most, 75% of individuals in target domain will also be created with transferred
	 * knowledge.
	 */
	private int transferPercent;

	private static int cfCounter = 0;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base);

		Parameter knowledgeFileName = base.push(P_KNOWLEDGE_FILE);
		String fileName = state.parameters.getString(knowledgeFileName, null);
		if(fileName == null)
		{
			state.output.fatal("Failed to load parameter", knowledgeFileName);
		}
		File kbFile = new File(fileName);
		if(kbFile.exists() == false)
		{
			state.output.fatal("Knowledge file does not exist: " + fileName, knowledgeFileName);
		}

		Parameter knowledgeExtraction = base.push(P_KNOWLEDGE_EXTRACTION);
		String extraction = state.parameters.getString(knowledgeExtraction, null);
		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);

		Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getIntWithDefault(transferPercentParam, null, -1);

		MySimpleCodeFragmentKB knowledgeBase = new MySimpleCodeFragmentKB(state, transferPercent);

		knowledgeBase.extractFrom(kbFile, extractionMethod);
		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("MYSimpleCodeFragmentBuilder loaded. Transfer percent: "
							 + transferPercent + ", extraction method: " + extractionMethod
							 + ", transfer percent: " + transferPercent);
	}


	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
			final GPNodeParent parent, final GPFunctionSet set,	final int argposition,
			final int requestedSize)
	{
		int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		int numToTransfer = Math.round(popSize * transferPercent / 100f);
		if(numToTransfer >= 0 && cfCounter < numToTransfer)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				cfCounter++;
				log(state, cf, cfCounter, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				// System.out.println("Loaded a CF: " + node.makeCTree(false, false, false));
				return node;
			}
			else
				log(state, null, cfCounter, knowledgeSuccessLogID);
	//		else
	//			System.out.println("CF is null");
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
	}

}
