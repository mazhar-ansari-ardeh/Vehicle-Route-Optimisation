package tl.knowledge.sst;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.PopulationUtils;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSavingGPHHState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SSTEvolutionState extends DMSSavingGPHHState
{

	public static final String P_BASE = "sst-state";

	/**
	 * The distance metric that the clearing procedure uses. Acceptable values are (case insensitive):
	 *  - phenotypic
	 *  - corrphenotypic
	 *  - hamphenotypic
	 */
	public static final String P_DISTANCE_METRIC = "distance-metric";

	/**
	 * The flag parameter to enable or disable the act of knowledge transfer from a source domain.
	 */
	public static final String P_ENABLE_TRANSFER = "enable-transfer";

	/**
	 * If {@code true}, GP will update the history of individuals that are discovered during the GP run.
	 * When this parameter is set to {@code false} but the {@code P_ENABLE_TRANSFER} parameter is set to {@code true},
	 * GP then will only rely on the transferred knowledge and not the knowledge that it gains during the GP run. The
	 * purpose of this parameter is measure the effect of the transferred knowledge and find out if any achieved
	 * improvements are from the transferred knowledge or the discovered knowledge. The default value of this parameter
	 * is {@code true}.
	 */
	public static final String P_ENABLE_EVO_HIST_UPDATE = "enable-evo-hist-update";
	protected boolean enableEvoHistUpdate;

	/**
	 * The path to the file or directory that contains GP populations.
	 */
	public static final String P_KNOWLEDGE_PATH = "knowledge-path";

	SituationBasedTreeSimilarityMetric metrics;

	/**
	 * The radius of the clearing method used for loading the source domain.
	 */
	public static final String P_TRANSFER_RADIUS = "transfer-clear-radius";
//	private double transferClearRadius;

	/**
	 * The capacity of the clearing method used for loading the source domain.
	 */
	public static final String P_TRANSFER_CAPACITY = "transfer-clear-capacity";

	/**
	 * The similarity threshold for saving the new individuals that are found during the
	 * search process.
	 */
	public static final String P_HISTORY_SIM_THRESHOLD = "history-sim-threshold";
	protected double historySimThreshold;

	/**
	 * The directory to which the population is logged at the end of each generation.
	 */
	public static final String P_POP_LOG_PATH = "pop-log-path";
	private String popLogPath;

	/**
	 * A boolean parameter that specifies if the class load transferred individuals or create a random pool of
	 * individuals and use them as the transferred items. The main goal of this parameter is to act as a control
	 * experiment to verify the effect of the transferred knowledge. This parameter is read only if the
	 * {@code P_ENABLE_TRANSFER} parameter is {@code true}. The value of the parameter is {@code false} to be
	 * backward-compatible.
	 */
	public static final String P_RAND_TRANSFER = "rand-transfer";

	/**
	 * Size of the decision-making situations.
	 */
	public final String P_DMS_SIZE = "dms-size";

	/**
	 * The individuals that are transferred from the source domain. This list is loaded once and is not updated
	 * afterwards.
	 */
	protected List<GPRoutingPolicy> transferredInds = new ArrayList<>();

	/**
	 * The individuals that are discovered in the target domain during the GP evolution in the target domain. This list
	 * is updated after each generation.
	 */
	protected ArrayList<GPRoutingPolicy> discoveredInds = new ArrayList<>();

	/**
	 * This is a temporary archive that will hold the individuals that are seen during genetic operators. For example,
	 * if crossover creates a new individual, this new individual will be stored here temporarily so that later
	 * crossover and mutation operations within the same generation can see it. This approach is taken because the
	 * archive of seen individuals is updated at the beginning of each generation to record their fitness value and
	 * therefore, any individuals created within the generation will not be seen.
	 */
	protected ArrayList<GPRoutingPolicy> tempInds = new ArrayList<>();

	PoolFilter filter;
	private int knowledgeSuccessLogID;
	protected int dmsSize;

	/**
	 * Gets the individuals that were loaded/transferred from the source domain. This function returns the exact
	 * individuals that were loaded and does not create a deep or shallow copy of then. As a result, any changes applied
	 * to the return value will affect the actual values.
	 * @return Individuals loaded from the source domain. In case the transfer is disabled, an empty list will be
	 * returned.
	 */
	public List<GPRoutingPolicy> getTransferredIndividuals()
	{
		return transferredInds;
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		base = new Parameter(P_BASE);

		knowledgeSuccessLogID = setupLogger(state, base, true);

		popLogPath = state.parameters.getString(base.push(P_POP_LOG_PATH), null);
		if(popLogPath == null)
		{
			state.output.fatal("Population log path cannot be null");
			return;
		}
		state.output.warning("Population log path: " + popLogPath);

		Parameter p = new Parameter("eval.problem.pool-filter");
		filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

		String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
		log(state, knowledgeSuccessLogID, true, "Similarity metric: " + metricParam + "\n");

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

		dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
		log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

		historySimThreshold = state.parameters.getDouble(base.push(P_HISTORY_SIM_THRESHOLD), null);
		if(historySimThreshold < 0)
			logFatal(state, knowledgeSuccessLogID, "Invalid history threshold: " + historySimThreshold + "\n");
		log(state, knowledgeSuccessLogID, true, "History threshold: " + historySimThreshold + "\n");

		metrics.setSituations(getInitialSituations().subList(0,	Math.min(getInitialSituations().size(), dmsSize)));

		setDMSSavingEnabled(false);

		enableEvoHistUpdate = parameters.getBoolean(base.push(P_ENABLE_EVO_HIST_UPDATE), null, true);
		log(state, knowledgeSuccessLogID, true, "Enable evolutionary history update: " + enableEvoHistUpdate + "\n");

//		if(!parameters.containsKey(base.push(P_ENABLE_TRANSFER)))
//			logFatal(this,knowledgeSuccessLogID,"The parameter " + P_ENABLE_TRANSFER + " not found");
		boolean enableTransfer = parameters.getBoolean(base.push(P_ENABLE_TRANSFER), null, true);
		log(this, knowledgeSuccessLogID, true, "Enable transfer: " + enableTransfer + "\n");
		if(!enableTransfer)
			return;

		loadTransferredInds(state, base);
	}

	private void loadTransferredInds(EvolutionState state, Parameter base)
	{
		double clearRadius = state.parameters.getDouble(base.push(P_TRANSFER_RADIUS), null);
		log(state, knowledgeSuccessLogID, true, "Clearing radius of transferred knowledge: " + clearRadius + "\n");

		int clearCapacity = state.parameters.getInt(base.push(P_TRANSFER_CAPACITY), null);
		log(state, knowledgeSuccessLogID, true, "Clearing capacity of transferred knowledge:" + clearCapacity + "\n");

		boolean randTransfer = state.parameters.getBoolean(base.push(P_RAND_TRANSFER), null, false);
		log(state, knowledgeSuccessLogID, true, "Rand transfer: " + randTransfer + "\n");

		List<Individual> inds;

		if(randTransfer)
		{
			inds = loadRandInds(clearRadius, clearCapacity);
		}
		else
		{
			String knowledgePath = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
			if (knowledgePath == null)
			{
				state.output.fatal("Knowledge path cannot be null");
				return;
			}
			log(state, knowledgeSuccessLogID, true, "Knowledge path: " + knowledgePath + "\n");

			try
			{
				inds = PopulationUtils.loadPopulations(state, knowledgePath, 0, 49, filter, metrics,
						clearRadius, clearCapacity, this, knowledgeSuccessLogID, true);
				if(inds == null || inds.isEmpty())
					throw new RuntimeException("Could not load the saved populations");
			} catch (IOException | ClassNotFoundException e)
			{
				e.printStackTrace();
				state.output.fatal("Failed to load the population from: " + knowledgePath);
				return;
			}
		}

		transferredInds.addAll(
				inds.stream().map(
						i -> new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0])).collect(Collectors.toList()));
	}

	private List<Individual> loadRandInds(double radius, int capacity)
	{
		List<Individual> inds = new ArrayList<>();
		for(int i = 0; i < 50 * 1024; i++)
			inds.add(this.population.subpops[0].species.newIndividual(this, 0));

		SimpleNichingAlgorithm.clearPopulation(inds, filter, metrics, radius, capacity);
		inds = inds.stream().filter(i -> i.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());

		return inds;
	}

	boolean isNew(Individual i, double similarityThreshold)
	{
		if(i == null)
			throw new RuntimeException("The individual cannot be null.");
		if(isSeenIn(i, tempInds,similarityThreshold))
			return false;
		if(isSeenIn(i, transferredInds, similarityThreshold))
			return false;
		boolean isSeen = false;
		if(enableEvoHistUpdate)
		{
			isSeen = isSeenIn(i, discoveredInds, similarityThreshold);
			if (!isSeen)
			{
				GPIndividual j = (GPIndividual) i.clone();
				// GP builders do not create individuals but GP nodes. As a result, cloning their product will not matter
				// because they their fitness will not be updated after evaluation.
				tempInds.add(new GPRoutingPolicy(filter, j.trees[0]));
			}
		}

		return !isSeen;
	}

	private boolean isSeenIn(Individual i, List<GPRoutingPolicy> pool, double similarityThreshold)
	{
		if(pool.isEmpty())
			return false;

		GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0]);

		for (GPRoutingPolicy gpRoutingPolicy : pool)
		{
			if (metrics.distance(gpRoutingPolicy, policy) <= similarityThreshold)
				return true;
		}

		return false;

//		pool.sort(Comparator.comparingDouble(j -> j.fitness.fitness()));
//		((MultiObjectiveFitness)(i).fitness).objectives[0] = pool.get(pool.size() - 1).fitness.fitness() + 1;
//		pool.add(i);
//		pool.sort(Comparator.comparingDouble(j -> j.fitness.fitness()));
//		// Reimplement this. Clearing the pool is not a good idea.
//		SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, similarityThreshold, nicheCapacity);
//		pool.remove(i);
//		return i.fitness.fitness() == Double.POSITIVE_INFINITY;
	}

	@Override
	public int evolve()
	{
		if (generation > 0)
			output.message("Generation " + generation);

		// EVALUATION
		statistics.preEvaluationStatistics(this);
		evaluator.evaluatePopulation(this);
		statistics.postEvaluationStatistics(this);

		/* Newly discovered individuals are inside the population so there is no need to for them any more. Also, since
		* the updateHistory method uses the isSeen method, which in turn reviews the tempInds, this archive should be
		* emptied.
		*/
		tempInds.clear();
		/*
		There is a minor issue to consider here. Because the initializer is based on building GP trees, rather than GP
		individuals, there is no way to discriminate between transferred and discovered individuals. As a result, this
		method invocation will also add individuals that may have been transferred from a source domain. This is not a
		big issue because it just adds one minor duplicity but will not affect the original idea.
		*/
		updateSearchHistory(population.subpops[0].individuals);

		finish = util.Timer.getCpuTime();
		duration = 1.0 * (finish - start) / 1000000000;

		writeToStatFile();

		start = util.Timer.getCpuTime();

		logPopulation(population);

		// SHOULD WE QUIT?
		if (evaluator.runComplete(this) && quitOnRunComplete) {
			output.message("Found Ideal Individual");
			return R_SUCCESS;
		}
		// SHOULD WE QUIT?
		if (generation == numGenerations-1) return R_FAILURE;

		if (exchangePopulationPreBreeding()) return R_SUCCESS;

		// BREEDING
		breed();

		// POST-BREEDING EXCHANGING
		exchangePopulationPostBreeding();

		// Generate new instances if needed
		rotateEvalModel();

		// INCREMENT GENERATION AND CHECKPOINT
		generation++;
		doCheckpoit();

		return R_NOTDONE;
	}

	protected void updateSearchHistory(Individual[] inds)
	{
		if(!enableEvoHistUpdate)
			return;
		for (Individual ind : inds)
		{
			SSTIndividual i = (SSTIndividual) ind;
			if (isSeenIn(ind, transferredInds, historySimThreshold))
			{
				i.setOrigin(IndividualOrigin.InitTransfer);
				continue;
			}
			if (isSeenIn(i, discoveredInds, historySimThreshold))
			{
				continue;
			}
			if(i.getOrigin() == null) // The origin could be crossover, mutation, ....
				i.setOrigin(IndividualOrigin.InitRandom);
			discoveredInds.add(new GPRoutingPolicy(filter, i.trees[0]));
		}
	}

	private void logPopulation(Population population)
	{
		assert population != null;
		Individual[] pop = population.subpops[0].individuals;
		assert pop != null;

		int popLogID = setupLogger(this, new File(popLogPath, "pop/Pop." + generation + ".csv").getAbsolutePath());
		log(this, popLogID, "Origin,Fitness,Tree\n");
		Arrays.stream(pop).map(i -> (SSTIndividual)i).sorted(Comparator.comparingDouble(j -> j.fitness.fitness())).forEach(
				i -> log(this, popLogID,
						i.getOrigin() + "," + i.fitness.fitness() + "," + i.trees[0].child.makeLispTree() + "\n")
		);
		closeLogger(this, popLogID);
	}

}
