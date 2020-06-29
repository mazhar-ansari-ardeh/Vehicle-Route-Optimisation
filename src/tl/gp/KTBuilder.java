package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tl.TLLogger;
import tl.gp.hash.VectorialAlgebraicHashCalculator;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gp.simplification.AlgebraicTreeSimplifier;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.KnowledgeExtractionMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class KTBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	/**
	 * When the knowledge source is a directory, this parameter specifies from which generation on
	 * the populations should be read. This parameter is inclusive. If the knowledge path is to a file, this parameter
	 * is ignored and can be omitted.
	 */
	public final String P_GENERATION_FROM = "from-generation";
	/**
	 * When the knowledge source is a directory, this parameter specifies the generation until which
	 * the populations should be read. This parameter is inclusive. If the knowledge path is to a file, this parameter
	 * is ignored and can be omitted.
	 */
	public final String P_GENERATION_TO = "to-generation";

	/**
	 * The percent of the top individuals of the source domain to consider for extraction of subtrees. For example,
	 * given a 1024 individuals from a source domain, if the value of this parameter is 0.2, then 202 individuals will
	 * be selected from the 1024 individuals with a tournament selection. The value of this parameter is in the range of
	 * [0, 1].
	 */
	public final String P_EXTEACTION_PERCENT = "extract-percent";

	/**
	 * The size of the tournament that is used for selecting the transferred subtrees. The selection is based on the
	 * frequency with which the subtrees appeared in the source domain. This parameter cannot be zero or negative.
	 */
	public final String P_TOURNAMENT_SIZE = "tournament-size";
	private int tournamentSize;

	/**
	 * A boolean parameter that if {@code true}, the loaded trees from the source domain will be simplified before
	 * subtree extraction.
	 */
	public final String P_SIMPLIFY = "simplify";
	private boolean simplify = true;

	/**
	 * Niche radius. All items within this radius of a niche center will be cleared. The niche radius is used for
	 * clearing the loaded population.
	 */
	public final String P_NICHE_RADIUS = "niche-radius";
//	double nicheRadius;

	/**
	 * The capacity of each niche that is used for clearing. This parameter is only used for clearing of the loaded
	 * population.
	 */
	public final String P_NICHE_CAPACITY = "niche-capacity";
//	int nicheCapacity;

	/**
	 * The distance metric that the clearing procedure uses. Acceptable values are (case insensitive):
	 *  - phenotypic
	 *  - corrphenotypic
	 *  - hamphenotypic
	 */
	private static final String P_DISTANCE_METRIC = "distance-metric";

	/**
	 * Size of the decision-making situations.
	 */
	public final String P_DMS_SIZE = "dms-size";

	/**
	 * The path to the file or directory that contains GP populations.
	 */
	public static final String P_KNOWLEDGE_PATH = "knowledge-path";

	/**
	 * Extraction method. The acceptable methods are {@code KnowledgeExtractionMethod.AllSubtrees},
	 * {@code KnowledgeExtractionMethod.RootSubtree} and {@code KnowledgeExtractionMethod.Root}.
	 */
	public static final String P_KNOWLEDGE_EXTRACTION = "knowledge-extraction";

	private int knowledgeSuccessLogID;

	/**
	 * The repository of extracted subtrees. The key is the hashcode of the subtree and the value is a pair of the
	 * subtree itself and its frequency.
	 */
	HashMap<Integer, Pair<GPNode, Integer>> repository = new HashMap<>();
	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);

//		nicheRadius = state.parameters.getDouble(base.push(P_NICHE_RADIUS), null);
//		log(state, knowledgeSuccessLogID, true, "Interim niche radius " + nicheRadius + "\n");

//		nicheCapacity = state.parameters.getInt(base.push(P_NICHE_CAPACITY), null);
//		log(state, knowledgeSuccessLogID, true, "Interim niche capacity " + nicheCapacity + "\n");

		int fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
		int toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
		log(state, knowledgeSuccessLogID, true, "Load surrogate pool from generation " + fromGeneration +
				", to generation " + toGeneration + "\n");

		String extraction = state.parameters.getString(base.push(P_KNOWLEDGE_EXTRACTION), null);
		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);
		log(state, knowledgeSuccessLogID, true, extractionMethod.toString() + "\n");

		double extractPercent = state.parameters.getDouble(base.push(P_EXTEACTION_PERCENT), null);
		if(extractPercent <= 0 || extractPercent > 1)
		{
			log(state, knowledgeSuccessLogID, true, "Invalid extraction percent: " + extractPercent);
			throw new RuntimeException("Invalid extraction percent: " + extractPercent);
		}
		log(state, knowledgeSuccessLogID, true, "Extraction percent: " + extractPercent);

		String kbFile = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
		if (kbFile == null)
		{
			state.output.fatal("Knowledge path cannot be null");
			return;
		}

		double nicheRadius = state.parameters.getDouble(base.push(P_NICHE_RADIUS), null);
		log(state, knowledgeSuccessLogID, true, "Niche radius " + nicheRadius + "\n");

		int nicheCapacity = state.parameters.getInt(base.push(P_NICHE_CAPACITY), null);
		log(state, knowledgeSuccessLogID, true, "Niche capacity " + nicheCapacity + "\n");

		Parameter p = new Parameter("eval.problem.pool-filter");
		PoolFilter filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

		String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
		log(state, knowledgeSuccessLogID, true, "Similarity metric: " + metricParam + "\n");
		SituationBasedTreeSimilarityMetric metrics;
		if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
			metrics = new CorrPhenoTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Phenotypic"))
			metrics = new PhenotypicTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Hamming"))
			metrics = new HammingPhenoTreeSimilarityMetric();
		else
		{
			state.output.fatal("Unknown distance metric");
			return;
		}

		int dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
		log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

		if(!(state instanceof DMSSaver))
		{
			String message = "Evolution state must be of type DMSaver for this builder\n";
			log(state, knowledgeSuccessLogID, message);
			state.output.fatal(message);
			return;
		}
		DMSSaver sstate = (DMSSaver) state;

		metrics.setSituations(sstate.getInitialSituations().subList(0,
				Math.min(sstate.getInitialSituations().size(), dmsSize)));
		sstate.setDMSSavingEnabled(false);

		List<Individual> inds;
		try
		{
			// I like a large and diverse KNN pool so I set the niche radius to zero so that policies that are very
			// similar and are potentially duplicate are discarded without being too strict about it.
			inds = PopulationUtils.loadPopulations(state, kbFile, fromGeneration, toGeneration, filter, metrics,
					nicheRadius, nicheCapacity, this, knowledgeSuccessLogID, true);
			if(inds == null || inds.isEmpty())
				throw new RuntimeException("Could not load the saved populations");

		} catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal("Failed to load the population from: " + kbFile);
			return;
		}

		simplify = state.parameters.getBoolean(base.push(P_SIMPLIFY), null, true);
		log(state, knowledgeSuccessLogID, true, "Simplify: " + simplify);

		tournamentSize = state.parameters.getInt(base.push(P_TOURNAMENT_SIZE), null);
		if(tournamentSize <= 0)
		{
			log(state, knowledgeSuccessLogID, true, "Tournament size must be a positive value: " + tournamentSize + "\n");
			throw new RuntimeException("Tournament size must be a positive value: " + tournamentSize + "\n");
		}
		log(state, knowledgeSuccessLogID, true, "Tournament size: " + tournamentSize + "\n");

		int populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		log(state, knowledgeSuccessLogID, true, "Population size: " + populationSize + "\n");
		int sampleSize = (int) Math.floor(populationSize * extractPercent);
		extractSubtrees(inds, extractionMethod, sampleSize, state);
	}

	private void extractSubtrees(List<Individual> inds, KnowledgeExtractionMethod extractionMethod,
								 int sampleSize, EvolutionState state)
	{
		inds.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
		List<GPIndividual> ginds = inds.stream().map(i -> (GPIndividual) i).collect(Collectors.toList());
		ginds = PopulationUtils.rankSelect(ginds, sampleSize);

		VectorialAlgebraicHashCalculator hc = new VectorialAlgebraicHashCalculator(state, 0, 100, 100000);
		AlgebraicTreeSimplifier ts = new AlgebraicTreeSimplifier(hc);

		for(GPIndividual i : ginds)
		{
			if(simplify)
			{
				log(state, knowledgeSuccessLogID, false, "Loaded tree before simplification: \n");
				log(state, knowledgeSuccessLogID, false, i.trees[0].child.makeLispTree() + "\n");
				ts.simplifyTree(state, i);
				log(state, knowledgeSuccessLogID, false, "Loaded tree after simplification: \n");
				log(state, knowledgeSuccessLogID, false, i.trees[0].child.makeLispTree() + "\n\n");
			}
			ArrayList<GPNode> allNodes = new ArrayList<>();
			switch (extractionMethod)
			{
				case ExactCodeFragment:
					throw new RuntimeException("The ExactCodeFragment extraction method is not supported.");
				case Root:
					allNodes.add(i.trees[0].child);
					break;
				case RootSubtree:
					allNodes.addAll(TreeSlicer.sliceRootChildrenToNodes(i, false));
					break;
				case AllSubtrees:
					allNodes.addAll(TreeSlicer.sliceAllToNodes(i, false));
					break;
			}

			for(GPNode node : allNodes)
			{
				int hashCode = node.rootedTreeHashCode();
				Pair<GPNode, Integer> pair = repository.get(hashCode);
				if (pair == null)
				{
					pair = new MutablePair<>(node, 1);
				} else
				{
					pair.setValue(pair.getValue() + 1);
				}
				repository.put(hashCode, pair);
			}
		}
	}
}
