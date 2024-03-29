package tl.knowledge.surrogate;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.surrogate.knn.BuggyAddOncePhenotypic;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the original implementation of the {@link SurEvalBuilder} class. I restored this version because the original
 * run of this class provided good results but then, a second run of the same experiment gave bad results. This
 * indicated that there was bug in the code and so, I restored this class for the debugging purpose. Since then,
 * {@link SurEvalBuilder} has changed a lot. The experiment that this class implements is equivalent to the experiment
 * SurEvalFullTree tp genFrom genTo metric knnRadius 1 0 1 20
 */
@Deprecated
public class BuggySurEvalBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	/**
	 * The path to the file or directory that contains GP populations.
	 */
	public static final String P_KNOWLEDGE_FILE = "knowledge-path";

	/**
	 * The percentage of initial population that is created from extracted knowledge. The value must
	 * be in range [0, 1].
	 */
	private static final String P_TRANSFER_PERCENT = "transfer-percent";
	private double transferPercent;

	private static final String P_SURR_LOG_PATH = "surr-log-path";

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
	 * Niche radius. All items within this radius of a niche center will be cleared.
	 */
	public final String P_NICHE_RADIUS = "niche-radius";

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
	private int knowledgeSuccessLogID;
	private static int cfCounter = 0;
	private int populationSize;
	private List<Individual> pop;
	private int interimPopLogID;
	public KNNSurrogateFitness surFitness;
	private PoolFilter filter;

	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base);

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

		String fileName = state.parameters.getString(base.push(P_KNOWLEDGE_FILE), null);
		if (fileName == null)
		{
			state.output.fatal("Knowledge file name cannot be null");
			return;
		}

		Parameter p = new Parameter("eval.problem.pool-filter");
		filter = (PoolFilter)(state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

		setupSurrogate(state, base, fileName);
		populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		((DMSSaver)state).setDMSSavingEnabled(false);
	}

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

		metrics = null;
		String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
		if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
			metrics = new CorrPhenoTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Phenotypic"))
			metrics = new PhenotypicTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Hamming"))
			metrics = new HammingPhenoTreeSimilarityMetric();
		else
			state.output.fatal("Unknown distance metric");

		int fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
		int toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
		log(state, knowledgeSuccessLogID, true, "Load surrogate pool from generation " + fromGeneration +
				", to generation " + toGeneration + "\n");

		double nicheRadius = state.parameters.getDouble(base.push(P_NICHE_RADIUS), null);
		log(state, knowledgeSuccessLogID, true, "Niche radius " + nicheRadius + "\n");

		DMSSaver sstate = (DMSSaver) state;
		surFitness = new KNNSurrogateFitness();
		surFitness.setSurrogateUpdatePolicy(new BuggyAddOncePhenotypic());
		surFitness.setMetric(metrics);
		List<ReactiveDecisionSituation> initSituations = sstate.getInitialSituations();
		initSituations = initSituations.subList(0, Math.min(initSituations.size(), DMS_SIZE));
		surFitness.setSituations(initSituations);
		surFitness.setFilter(filter);
		List<Individual> inds;
		try
		{
			inds = PopulationUtils.loadPopulations(state, kbFile, fromGeneration, toGeneration, filter, metrics,
					nicheRadius, 1,	this, surPoolLogID, true);
			if(inds == null || inds.isEmpty())
				throw new RuntimeException("Failed to load any individuals.");
		} catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal("Failed to load the population from: " + kbFile);
			return;
		}
		surFitness.updateSurrogatePool(inds.toArray(new Individual[0]), "s:gen_49");
		log(state, surPoolLogID, surFitness.logSurrogatePool());
		closeLogger(state, surPoolLogID);

		sstate.setDMSSavingEnabled(false);
	}

	void createInitPop(final EvolutionState state, final GPType type, final int thread,
					   final GPNodeParent parent, final GPFunctionSet set, final int argposition,
					   final int requestedSize)
	{
		pop = new ArrayList<>(populationSize);

		for (int k = 0; k < 10; k++)
		{
			ArrayList<SuGPIndividual> tempPop = new ArrayList<>();
			for (int i = 0; i < populationSize; i++)
			{
				GPNode root = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
				SuGPIndividual ind = SuGPIndividual.asGPIndividual(root, -1);
				((MultiObjectiveFitness) ind.fitness).objectives[0] = surFitness.fitness(ind);
				ind.setSurFit(ind.fitness.fitness());

				tempPop.add(ind);
			}
			tempPop.forEach(ind -> log(state, interimPopLogID, ind.getSurFit() + "," + ind.fitness.fitness()
					+ "," + ind.trees[0].child.makeLispTree() + "\n"));
			pop.addAll(tempPop);
			pop.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
			SimpleNichingAlgorithm.clearPopulation(pop, filter, metrics, 0d, 1);
			log(state, interimPopLogID, ",,Iteration " + k + " finished.\n\n");

			pop = pop.stream().filter(i -> i.fitness.fitness() != Double.POSITIVE_INFINITY)
					.collect(Collectors.toList());
			if(k == 9 && pop.size() < transferPercent * populationSize + 1)
				--k; // Don't let the loop exit until enough individuals are created.
		}

		pop.forEach(ind -> log(state, interimPopLogID, ((SuGPIndividual)ind).getSurFit() + "," + ind.fitness.fitness()
				+ "," + ((GPIndividual)ind).trees[0].child.makeLispTree() + "\n"));
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
			GPIndividual ind = (GPIndividual) pop.remove(0);
			double fit = ind.fitness.fitness();
			GPNode root = GPIndividualUtils.stripRoots(ind).get(0);

			cfCounter++;
			log(state, knowledgeSuccessLogID, cfCounter + ": \t" + fit + "," + root.makeLispTree() + "\n");
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
