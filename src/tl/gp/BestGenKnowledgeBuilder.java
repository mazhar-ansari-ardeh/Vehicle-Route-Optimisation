package tl.gp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import ec.*;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.BestGenCodeFragmentKB;

public class BestGenKnowledgeBuilder extends HalfBuilder
{
	private static final long serialVersionUID = 1L;

// 	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	public static final String P_KNOWLEDGE_FOLDER = "knowledge-folder";

	public static final String P_K = "k";


	private static KnowledgeExtractor extractor = null;

	private int knowledgeSuccessLogID;

	/**
	 * Percentage of the source domain knowledge to be transferred. In this implementation, this
	 * percentage is used both on source and target domains so that for example, a value of .75
	 * means that 75% of individuals in source domain will be considered for knowledge extraction
	 * and also, at most, 75% of individuals in target domain will also be created with transferred
	 * knowledge.
	 */
	private int k;


	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter knowledgeFolderParam = base.push(P_KNOWLEDGE_FOLDER);
		String knowledgeFolder = state.parameters.getString(knowledgeFolderParam, null);
// 		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);

		Parameter kParam = base.push(P_K);
		k = state.parameters.getIntWithDefault(kParam, null, -1);

		BestGenCodeFragmentKB knowledgeBase = new BestGenCodeFragmentKB(k);

		knowledgeBase.extractFrom(Paths.get(knowledgeFolder), ".bin");
		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("BestGenKnowledgeBuilder loaded. Transfer percent: " + k);

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
		if(transferCount < popSize)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				transferCount++;
				log(state, cf, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				return node;
			}
			else
				log(state, null, knowledgeSuccessLogID);
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
	}

	private void log(EvolutionState state, CodeFragmentKI it, int logID)
	{
		state.output.println(transferCount + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}
}
