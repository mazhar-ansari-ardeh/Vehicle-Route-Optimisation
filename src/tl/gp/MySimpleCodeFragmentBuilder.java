package tl.gp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.MySimpleCodeFragmentKB;

public class MySimpleCodeFragmentBuilder extends HalfBuilder
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_KNOWLEDGE_TOURNAMENT_SIZE = "knowledge-tournament-size";

	public static final String P_FILTER_SIZE = "knowledge-filter-size";

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


	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

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

//		Parameter problemParam = new Parameter("pop.subpop.0.species.fitness");
//		Fitness fitness = (Fitness) state.parameters.getInstanceForParameter(problemParam,
//				null, Fitness.class);
//		fitness.setup(state, problemParam);

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

		try {
			Parameter knowledgeLogFileNameParam = base.push(P_KNOWLEDGE_LOG_FILE_NAME);
			String knowledgeLogFile = state.parameters.getString(knowledgeLogFileNameParam, null);
			File successKnLog = new File(knowledgeLogFile + ".succ.log");
			if(successKnLog.exists())
				successKnLog.delete();

			Path pathToSuccFile = successKnLog.toPath();
			Path pathToSuccDir = pathToSuccFile.getParent();
			if(pathToSuccDir != null)
			{
				File statDirFile = pathToSuccDir.toFile();
				if(statDirFile.exists() == false && statDirFile.mkdirs() == false)
					state.output.fatal("Failed to create stat directory: "
									   + pathToSuccDir.toString());
			}

			Path pathToFailFile = successKnLog.toPath();
			Path pathToFailDir = pathToFailFile.getParent();
			if(pathToFailDir != null)
			{
				File statDirFile = pathToFailDir.toFile();
				if(statDirFile.exists() == false && statDirFile.mkdirs() == false)
					state.output.fatal("Failed to create stat directory: "
									   + pathToFailDir.toString());
			}

			successKnLog.createNewFile();

			knowledgeSuccessLogID = state.output.addLog(successKnLog, false);
		}
		 catch (IOException e) {
				state.output.fatal("Failed to create knowledge log file in CodeFragmentBuilder: "
						+ e.getStackTrace().toString());
			}
	}


	private int transferCount = 0;
	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
			final GPNodeParent parent, final GPFunctionSet set,	final int argposition,
			final int requestedSize)
	{
		int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		int numToTransfer = Math.round(popSize * transferPercent / 100f);
		if(numToTransfer >= 0 && transferCount < numToTransfer)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				cfCounter++;
				log(state, cf, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				transferCount++;
				// System.out.println("Loaded a CF: " + node.makeCTree(false, false, false));
				return node;
			}
			else
				log(state, null, knowledgeSuccessLogID);
	//		else
	//			System.out.println("CF is null");
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
	}

	private static int cfCounter = 0;

	private void log(EvolutionState state, CodeFragmentKI it, int logID)
	{
		state.output.println(cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}
}
