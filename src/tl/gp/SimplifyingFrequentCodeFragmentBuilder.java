package tl.gp;

import java.nio.file.*;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.TLLogger;
import tl.knowledge.*;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.SimplifyingFrequentCodeFragmentKB;

/**
 *
 * @author mazhar
 *
 */
public class SimplifyingFrequentCodeFragmentBuilder 	extends HalfBuilder
											implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The file that contains knowledge. In case of this class, the file must contain a population
	 * of GP individuals to extract knowledge from. <p>
	 *
	 * <b>Important: Make sure that individuals in this file have fitness values in the test
	 * phase.</b>
	 */
	public static final String P_KNOWLEDGE_DIRECTORY = "knowledge-directory";

	/**
	 * Extraction method. The acceptable methods are {@code KnowledgeExtractionMethod.AllSubtrees},
	 * {@code KnowledgeExtractionMethod.RootSubtree} and {@code KnowledgeExtractionMethod.Root}.
	 */
	public static final String P_KNOWLEDGE_EXTRACTION = "knowledge-extraction";

	/**
	 * The percentage of initial population that is created from extracted knowledge. The value must
	 * be in range (0, 1].
	 */
	public static final String P_TRANSFER_PERCENT = "transfer-percent";

	/**
	 * The percentage of top performer individuals in source domain to consider for knowledge
	 * extraction. The value must be in range (0, 1].
	 */
	public static final String P_EXTRACT_PERCENT = "extract-percent";

	/**
	 * Minimum allowed size of code fragments to use, inclusive. A value of zero or less means
	 * anything.
	 */
	public static final String P_MIN_CF_DEPTH = "min-cf-depth";

	/**
	 * Maximum allowed size of code fragments to use, exclusive. A value of zero or less means
	 * everything.
	 */
	public static final String P_MAX_CF_DEPTH = "max-cf-depth";

	/**
	 * A boolean parameter that if true, the knowledge base will simplify and remove redundant
	 * subtrees from the GP individuals before extracting subtrees.
	 */
	public static final String P_SIMPLIFY = "simplify";

	/**
	 * {@code FrequentCodeFragmentKB} can extract its knowledge from a set of population files in a
	 * directory. This parameter specifies what percent of most recent generations should be
	 * considered. The value of this parameter is must be in the range [0, 1].
	 */
	public static final String P_GENERATION_PERCENT = "generation-percent";

	public static final String P_KNOWLEDGE_FREQ_TOURNAMENT_SIZE = "know-freq-tournament-size";

	private static KnowledgeExtractor extractor = null;

	private static SimplifyingFrequentCodeFragmentKB knowledgeBase = null;

	public static SimplifyingFrequentCodeFragmentKB getKnowledgeBase()
	{
		return knowledgeBase;
	}

	private int knowledgeSuccessLogID;

	/**
	 * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
	 * population that is transfered from extracted knowledge. The value must be in range (0, 1].
	 */
	private double transferPercent;

	private static int cfCounter = 0;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base);

		Parameter p = base.push(P_KNOWLEDGE_DIRECTORY);
		String directoryName = state.parameters.getString(p, null);
		if(directoryName == null)
		{
			state.output.fatal("Failed to load parameter", p);
		}
		Path kbDirectory = Paths.get(directoryName);
		if(kbDirectory.toFile().exists() == false || kbDirectory.toFile().isDirectory() == false)
		{
			state.output.fatal("Knowledge directory does not exist: " + directoryName, p);
		}

		p = base.push(P_KNOWLEDGE_EXTRACTION);
		String extraction = state.parameters.getString(p, null);
		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);

		p = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getDouble(p, null);

		p = base.push(P_MIN_CF_DEPTH);
		int minDepth = state.parameters.getInt(p, null);

		p = base.push(P_MAX_CF_DEPTH);
		int maxDepth = state.parameters.getInt(p, null);

		p = base.push(P_EXTRACT_PERCENT);
		double extractPercent = state.parameters.getDouble(p, null);

		p = base.push(P_SIMPLIFY);
		boolean simplyfy = state.parameters.getBoolean(p, null, false);

		double genPercent = state.parameters.getIntWithDefault(base.push(P_GENERATION_PERCENT)
																, null, 1);

		int tournamentSize = state.parameters.getIntWithDefault(base.push(P_KNOWLEDGE_FREQ_TOURNAMENT_SIZE), null, 10);
		if(genPercent < 0 || genPercent > 1)
			state.output.fatal(P_GENERATION_PERCENT + " must have a value in the range [0, 1]");

		knowledgeBase =	new SimplifyingFrequentCodeFragmentKB(state, extractPercent, genPercent
												, minDepth, maxDepth, 0, simplyfy, tournamentSize); // TODO: Use a correct thread number.


		knowledgeBase.extractFrom(kbDirectory, extractionMethod);


		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("DepthedFrequentSimpleCodeFragmentBuilder loaded. Transfer percent: "
							 + transferPercent + ", extraction method: " + extractionMethod
							 + ", min depth: " + minDepth + ", max depth: " + maxDepth);
	}


	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
			final GPNodeParent parent, final GPFunctionSet set,	final int argposition,
			final int requestedSize)
	{
		int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		int numToTransfer = (int) Math.round(popSize * transferPercent);
		if(numToTransfer >= 0 && cfCounter < numToTransfer)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				cfCounter++;
				log(state, cf, cfCounter, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				return node;
			}
			else
				log(state, null, cfCounter, knowledgeSuccessLogID);
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
							type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
							type,thread,parent,argposition,set);
	}

}
