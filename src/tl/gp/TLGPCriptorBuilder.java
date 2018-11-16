package tl.gp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
import ec.gp.GPFunctionSet;
import ec.gp.GPNode;
import ec.gp.GPNodeParent;
import ec.gp.GPType;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.TLGPCriptorKB;

/**
 * This class, along with the {@code TLGPCriptorKB} and {@code TLGPCriptorMutation} classes,
 * implement the idea in paper "<i>Reusing Extracted Knowledge in Genetic Programming to Solve
 * Complex Texture Image Classification Problems</i>".
 * @author mazhar
 */
public class TLGPCriptorBuilder extends HalfBuilder
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	public final static String P_KNOWLEDGE_PROBABILITY = "knowledge-probability";

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	private double knowledgeProbability;

	private int knowledgeSuccessLogID;

	private static KnowledgeExtractor extractor = null;

	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return extractor;
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter knowledgeProbabilityParam = base.push(P_KNOWLEDGE_PROBABILITY);
		knowledgeProbability = state.parameters.getDouble(knowledgeProbabilityParam, null);

		TLGPCriptorKB knowledgeBase = new TLGPCriptorKB(state);

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
		knowledgeBase.extractFrom(kbFile, KnowledgeExtractionMethod.RootSubtree);
		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("TLGPCriptorBuilder loaded. Knowledge probability: "
							 + knowledgeProbability);

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

	private GPNode getKnowlege(EvolutionState state, int thread, GPNodeParent parent, int argposition)
	{
		if(extractor.hasNext() == false)
			state.output.fatal("KnowledgeBase is empty");

		CodeFragmentKI item = (CodeFragmentKI) extractor.getNext();
		cfCounter++;
		item.incrementCounter();
		log(state, item, knowledgeSuccessLogID);
		GPNode node = item.getItem();
		node.argposition = (byte) argposition;
		node.parent = parent;

		return node;
	}

	@Override
	protected GPNode growNode(EvolutionState state, int current, int max, GPType type, int thread,
			GPNodeParent parent, int argposition, GPFunctionSet set)
	{
		if(current == 1) // The TLGPCriptor only is only applicable to children of the root
		{
			if(state.random[thread].nextDouble() < knowledgeProbability)
			{
				return getKnowlege(state, thread, parent, argposition);
			}
		}

		return super.growNode(state, current, max, type, thread, parent, argposition, set);
	}

	@Override
	protected GPNode fullNode(EvolutionState state, int current, int max, GPType type, int thread,
			GPNodeParent parent, int argposition, GPFunctionSet set)
	{
		if(current == 1) // The TLGPCriptor only is only applicable to children of the root
		{
			if(state.random[thread].nextDouble() < knowledgeProbability)
			{
				return getKnowlege(state, thread, parent, argposition);
			}
		}
		return super.fullNode(state, current, max, type, thread, parent, argposition, set);
	}

	private static int cfCounter = 0;

	private void log(EvolutionState state, CodeFragmentKI it, int logID)
	{
		state.output.println(cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}
}
