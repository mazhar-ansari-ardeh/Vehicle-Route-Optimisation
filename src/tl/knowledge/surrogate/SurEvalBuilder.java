package tl.knowledge.surrogate;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;
import tl.knowledge.surrogate.knn.UnboundedUpdatePolicy;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class performs the Surrogate-Evaluated Fulltree transfer. This method gets a path to a directory or file that
 * contains knowledge, loads the population from it, performs a clearing on it and forms a surrogate pool from the
 * cleared population. The experiment creates an intermediate population of 10 times the size of the originial
 * population, evaluates the intermediate population with the surrogate, removes duplicates with a clearing method and
 * then, initialise a percentage of target domains from the top individuals of the cleared population.
 *
 * The basic idea of this class/experiment is to create a set of individuals that are potentially good and are also as
 * diverse as possible, hoping that the diversity and the good quality of the individuals will help to create an initial
 * state that will have a lasting effect.
 *
 * This function has the following parameters:
 * 1. Percentage of the target population to initialise,
 * 2. Similarity metric:
 *       2.1. phenotypic
 *       2.2. corrphenotypic
 *       2.3. hamming
 * 3. Generation of source domain from which to start loading populations (inclusive)
 * 4. Generation of source domain until which which to start loading populations (inclusive)
 * 5. Niche radius.
 */
public class SurEvalBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	/**
	 * The path to the file or directory that contains GP populations.
	 */
	public static final String P_KNOWLEDGE_PATH = "knowledge-path";

	/**
	 * The percentage of initial population that is created from extracted knowledge. The value must
	 * be in range [0, 1].
	 */
	private static final String P_TRANSFER_PERCENT = "transfer-percent";
	private double transferPercent;

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
	 * Niche radius. All items within this radius of a niche center will be cleared. The niche radius is used only for
	 * clearing the newly-created intermediate pool.
	 */
	public final String P_NICHE_RADIUS = "niche-radius";
	double nicheRadius;

	/**
	 * The capacity of each niche that is used for clearing. This parameter is only used during the clearing process of
	 * the intermediate population.
	 */
	public final String P_NICHE_CAPACITY = "niche-capacity";
	int nicheCapacity;

	/**
	 * Disable surrogate evaluation of the newly created individuals. If this boolean parameter is true, newly-created
	 * trees will not be evaluated with the surrogate method and a fitness value of -1 will be
	 * assigned to them. This feature acts as a control measure for testing to see if the results come from the
	 * surrogate or not. The default value of this parameter is {@code false}.
	 */
	public final String P_DISABLE_SUR_EVAL = "disable-sur-eval";
	private boolean disableSurEval;

	/**
	 * The distance metric that KNN uses. Acceptable values are (case insensitive):
	 *  - phenotypic
	 *  - corrphenotypic
	 *  - hamphenotypic
	 * Some updating policies, such as CorrEntropy or Entropy, may have their own metric and will override this
	 * parameter.
	 */
	private static final String P_DISTANCE_METRIC = "distance-metric";
	private SituationBasedTreeSimilarityMetric metrics;

	private static final int DMS_SIZE = 20;

	private static final String P_SURR_LOG_PATH = "surr-log-path";
	private int knowledgeSuccessLogID;
	private int interimPopLogID;

	private static int cfCounter = 0;
	private int populationSize;
	private List<Individual> pop;
	public KNNSurrogateFitness surFitness;

	private void setupSurrogate(EvolutionState state, Parameter base, String kbFile)
	{
		String surLogPath = state.parameters.getString(base.push(P_SURR_LOG_PATH), null);
		if(surLogPath == null)
		{
			state.output.fatal("Surrogate log path cannot be null");
			return;
		}
		state.output.warning("Surrogate log path: " + surLogPath);

		int surPoolLogID = setupLogger(state, new File(surLogPath, "surr/SurrogatePool.0.csv").getAbsolutePath());
		interimPopLogID = setupLogger(state, new File(surLogPath, "pop/InterimPop.0.csv").getAbsolutePath());
		log(state, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");

		int fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
		int toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
		log(state, knowledgeSuccessLogID, true, "Load surrogate pool from generation " + fromGeneration +
				 									 ", to generation " + toGeneration + "\n");
		DMSSaver sstate = (DMSSaver) state; // This is checked in the 'setup' method.
		surFitness = new KNNSurrogateFitness();

		// Almost all other update policy methods do some modifications to the pool. However, in this experiment, I
		// don't want this because I want to have a pool that is as large as possible. I perform all modifications
		// myself and before passing the pool to the KNN and its update policy.
		surFitness.setSurrogateUpdatePolicy(new UnboundedUpdatePolicy());
		surFitness.setMetric(metrics);
		surFitness.setSituations(sstate.getInitialSituations().subList(0,
				Math.min(sstate.getInitialSituations().size(), DMS_SIZE)));
		List<Individual> inds;
		try
		{
			// I like a large and diverse KNN pool so I set the niche radius to zero so that policies that are very
			// similar and are potentially duplicate are discarded without being too strict about it.
			inds = PopulationUtils.loadPopulations(state, kbFile, fromGeneration, toGeneration, metrics, 0,
											1, this, knowledgeSuccessLogID, true);
		} catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal("Failed to load the population from: " + kbFile);
			return;
		}

		surFitness.setFilter(new ExpFeasibleNoRefillPoolFilter());
		surFitness.updateSurrogatePool(inds.toArray(new Individual[0]), "s:gen_49");

		log(state, surPoolLogID, surFitness.logSurrogatePool());
		closeLogger(state, surPoolLogID);
	}

	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);

		Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getDouble(transferPercentParam, null);
		if(transferPercent < 0 || transferPercent > 1)
		{
			log(state, knowledgeSuccessLogID, "Transfer percent must be in [0, 1]");
			state.output.fatal("Transfer percent must be in [0, 1]");
		}
		else
		{
			log(state, knowledgeSuccessLogID, "Transfer percent: " + transferPercent);
		}

		nicheRadius = state.parameters.getDouble(base.push(P_NICHE_RADIUS), null);
		log(state, knowledgeSuccessLogID, true, "Niche radius " + nicheRadius + "\n");

		nicheCapacity = state.parameters.getInt(base.push(P_NICHE_CAPACITY), null);
		log(state, knowledgeSuccessLogID, true, "Niche capacity " + nicheCapacity + "\n");

		populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);

		if(!(state instanceof DMSSaver))
		{
			String message = "Evolution state must be of type DMSaver for this builder\n";
			log(state, knowledgeSuccessLogID, message);
			state.output.fatal(message);
		}
		DMSSaver sstate = (DMSSaver) state;

		metrics = null;
		String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
		log(state, knowledgeSuccessLogID, "Similarity metric: " + metricParam + "\n");
		if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
			metrics = new CorrPhenoTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Phenotypic"))
			metrics = new PhenotypicTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Hamming"))
			metrics = new HammingPhenoTreeSimilarityMetric();
		else
			state.output.fatal("Unknown distance metric");

		metrics.setSituations(sstate.getInitialSituations().subList(0,
				Math.min(sstate.getInitialSituations().size(), DMS_SIZE)));
		sstate.setDMSSavingEnabled(false);

		this.disableSurEval = state.parameters.getBoolean(base.push(P_DISABLE_SUR_EVAL), null, false);
		log(state, knowledgeSuccessLogID, true, "disableSurEval: " + disableSurEval + "\n");
		if(disableSurEval)
			return;

		String knowledgePath = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
		if (knowledgePath == null)
		{
			state.output.fatal("Knowledge path cannot be null");
			return;
		}

		setupSurrogate(state, base, knowledgePath);
	}

	void createInitPop(final EvolutionState state, final GPType type, final int thread,
					   final GPNodeParent parent, final GPFunctionSet set, final int argposition,
					   final int requestedSize)
	{
		pop = new ArrayList<>(populationSize);

		for (int k = 0; k < 10; k++)
		{
			for (int i = 0; i < populationSize; i++)
			{
				GPNode root = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
				SuGPIndividual ind = SuGPIndividual.asGPIndividual(root, -1);
				if(disableSurEval)
				{
					((MultiObjectiveFitness) ind.fitness).objectives[0] = -1;
					ind.setSurFit(-1);
				} else
				{
					((MultiObjectiveFitness) ind.fitness).objectives[0] = surFitness.fitness(ind);
					ind.setSurFit(ind.fitness.fitness());
				}

				pop.add(ind);
			}
			pop.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
			SimpleNichingAlgorithm.clearPopulation(pop, metrics, nicheRadius, nicheCapacity);
			pop.forEach(ind -> log(state, interimPopLogID,
					((SuGPIndividual)ind).getSurFit() + "," + ind.fitness.fitness()
					+ "," + ((GPIndividual)ind).trees[0].child.makeLispTree() + "\n"));
			log(state, interimPopLogID, ",,Iteration " + k + " finished.\n\n");

			pop = pop.stream().filter(i -> i.fitness.fitness() != Double.POSITIVE_INFINITY)
					  		  .collect(Collectors.toList());
			if(k == 9 && pop.size() < transferPercent * populationSize + 1)
				--k; // Don't let the loop exit until enough individuals are created.
		}

		closeLogger(state, interimPopLogID);

		pop.sort((Comparator.comparingDouble(i -> i.fitness.fitness())));
		pop = pop.subList(0, (int) (transferPercent * populationSize));
	}

	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
								final GPNodeParent parent, final GPFunctionSet set, final int argposition,
								final int requestedSize)
	{
		if(pop == null)
			createInitPop(state, type, thread, parent, set, argposition, requestedSize);

		if(cfCounter < populationSize * transferPercent && !pop.isEmpty())
		{
			GPNode root = GPIndividualUtils.stripRoots((GPIndividual) pop.remove(0)).get(0);

			cfCounter++;
			log(state, knowledgeSuccessLogID, cfCounter + ": \t" + root.makeLispTree() + "\n\n");
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
