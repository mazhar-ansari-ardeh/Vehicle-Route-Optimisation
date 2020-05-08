package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.SurrogatedGPHHEState;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClearedFullTreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	/**
	 * The path to the file that contains saved populations. This can be a path to a file or directory.
	 */
	public static final String P_KNOWLEDGE_PATH = "knowledge-path";

	/**
	 * The percentage of initial population that is created from extracted knowledge. The value must
	 * be in range [0, 1].
	 */
	private static final String P_TRANSFER_PERCENT = "transfer-percent";
	private double transferPercent;

	private static final int DMS_SIZE = 20;

	private int knowledgeSuccessLogID;


	/**
	 * The distance metric that KNN uses. Acceptable values are (case insensitive):
	 *  - phenotypic
	 *  - corrphenotypic
	 *  - hamphenotypic
	 * Some updating policies, such as CorrEntropy or Entropy, may have their own metric and will override this
	 * parameter.
	 */
	private static final String P_DISTANCE_METRIC = "distance-metric";

	/**
	 * Total number of generations on the source domain. This parameter is counted from 1.
	 */
	public final String P_NUM_GENERATIONS = "num-generations";
	private int numGenerations = -1;

	/**
	 * When the knowledge source is a directory, this parameter specifies from which generation on
	 * the populations should be read. This parameter is inclusive.
	 */
	public final String P_GENERATION_FROM = "from-generation";
	private int fromGeneration = -1;

	/**
	 * When the knowledge source is a directory, this parameter specifies the generation until which
	 * the populations should be read. This parameter is inclusive.
	 */
	public final String P_GENERATION_TO = "to-generation";
	private int toGeneration = -1;

	/**
	 * Niche radius. All items within this radius of a niche center will be cleared.
	 */
	public final String P_NICHE_RADIUS = "niche-radius";
	private int nicheRadius;

	private static int cfCounter = 0;
	private List<Individual> pool = new ArrayList<>();
	private int populationSize;

	private List<Individual> loadPopulations(EvolutionState state, String inputFileNamePath,
														 SituationBasedTreeSimilarityMetric metrics,
														 int logID) throws IOException, ClassNotFoundException
	{
		File f = new File(inputFileNamePath);

		List<Individual> pool = new ArrayList<>();
		if (f.isDirectory())
		{
			if (fromGeneration <= -1 || toGeneration <= -1)
			{
				log(state, logID, "Generation range is invalid: " + fromGeneration
						+ " to " + toGeneration + "\n");
				state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
						+ toGeneration);
			}
			for (int i = toGeneration; i >= fromGeneration; i--)
			{
				File file = Paths.get(inputFileNamePath, "population.gen." + i + ".bin").toFile();
				if(!file.exists())
				{
					log(state, logID, "The file " + file.toString() + " does not exist. Ignoring.\n");
					continue;
				}
				Population p = PopulationUtils.loadPopulation(file);
				PopulationUtils.sort(p);
				pool.addAll(Arrays.asList(p.subpops[0].individuals));
				SimpleNichingAlgorithm.clearPopulation(pool, metrics, nicheRadius, 1);
				pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
			}
		}

		log(state, logID, "pool size: " + pool.size() + "\n");
		pool.forEach(i ->
				log(state, logID, i.fitness.fitness() + ", " + ((GPIndividual)i).trees[0].child.makeLispTree() + "\n"));

		return pool;
	}


	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base);

		Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getDouble(transferPercentParam, null);
		if (transferPercent < 0 || transferPercent > 1)
		{
			log(state, knowledgeSuccessLogID, "Transfer percent must be in [0, 1]");
			state.output.fatal("Transfer percent must be in [0, 1]");
		} else {
			log(state, knowledgeSuccessLogID, "Transfer percent: " + transferPercent);
			state.output.warning( "Transfer percent: " + transferPercent);
		}

		fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
		state.output.warning("From generation: " + fromGeneration);

		toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
		state.output.warning("To generation: " + toGeneration);

		numGenerations = state.parameters.getInt(base.push(P_NUM_GENERATIONS), null);
		state.output.warning("Number of generations on source domain: " + numGenerations);

		String fileName = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
		if (fileName == null)
		{
			state.output.fatal("Knowledge path cannot be null");
			return;
		}

		SituationBasedTreeSimilarityMetric metrics = null;
		String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
		if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
			metrics = new CorrPhenoTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Phenotypic"))
			metrics = new PhenotypicTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Hamming"))
			metrics = new HammingPhenoTreeSimilarityMetric();
		else
			state.output.fatal("Unknown distance metric");
		state.output.warning("Similarity metrics: " + metricParam);

		nicheRadius = state.parameters.getInt(base.push(P_NICHE_RADIUS), null);
		if(nicheRadius < 0)
			state.output.fatal("Niche radius cannot be negative");
		state.output.warning("Niche radius: " + nicheRadius);

		if(!(state instanceof DMSSaver))
			state.output.fatal("Evolution state is not of the DMSSaver type.");

		DMSSaver sstate = (DMSSaver) state;
		List<ReactiveDecisionSituation> situations =
				sstate.getInitialSituations().subList(0, Math.min(sstate.getInitialSituations().size(), DMS_SIZE));
		if (metrics != null)
		{
			metrics.setSituations(situations);
		}
		else
			state.output.fatal("Initial DMS is null");

		try
		{
			pool = loadPopulations(state, fileName, metrics, knowledgeSuccessLogID);
		} catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal("Failed to load the populations.");
		}

		sstate.setDMSSavingEnabled(false);

		populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
	}

	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
								final GPNodeParent parent, final GPFunctionSet set, final int argposition,
								final int requestedSize)
	{
		if (cfCounter < populationSize * transferPercent && !pool.isEmpty())
		{
			Individual ind = pool.remove(0);
			double fitness = ind.fitness.fitness();
			GPNode root = GPIndividualUtils.stripRoots((GPIndividual) ind).get(0);

			cfCounter++;
			log(state, knowledgeSuccessLogID, cfCounter + ": \t" + fitness + ", " +
					root.makeCTree(false, true, true) + "\n\n");
			root.parent = parent;
			root.argposition = (byte) argposition;
			return root;
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
					type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
					type,thread,parent,argposition,set);
	}
}
