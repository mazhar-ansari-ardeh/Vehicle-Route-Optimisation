package gphhucarp.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.Initializer;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gputils.TerminalERCEvolutionState;
import gputils.terminal.DoubleERC;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The evolution state of evolving routing policy with GPHH.
 *
 * @author gphhucarp
 *
 */
public class GPHHEvolutionState extends TerminalERCEvolutionState implements TLLogger<GPNode>
{
	/**
	 * Statistics to store.
	 */
	public static final String POP_PROG_SIZE = "pop-prog-size";
	public static final String POP_FITNESS = "pop-fitness";

	/**
	 * Read the file to specify the terminals.
	 */
	public static final String P_TERMINALS_FROM = "terminals-from";
	public static final String P_INCLUDE_ERC = "include-erc";

	/**
	 * Whether to rotate the evaluation model or not.
	 */
	public static final String P_ROTATE_EVAL_MODEL = "rotate-eval-model";

	public static final String P_CLEAR = "clear";
	private boolean clear;

	protected String terminalFrom;
	protected boolean includeErc;
	protected boolean rotateEvalModel;

	protected long jobSeed;

    protected Map<String, DescriptiveStatistics> statisticsMap;
	protected File statFile;
	protected String statDir;

	private TreeMap<Individual, List<DecisionSituation>> seenSituations = new TreeMap<>();

	public void resetSeenSituations()
	{
		seenSituations.clear();
	}

	public Map<String, DescriptiveStatistics> getStatisticsMap() {
		return statisticsMap;
	}

	public DescriptiveStatistics getStatistics(String key) {
		return statisticsMap.get(key);
	}

	public long getJobSeed() {
		return jobSeed;
	}

	protected long start, finish;
	protected double duration;

	public void initStatFile() {
//		statFile = new File(statDir == null ? "." : statDir, "job." + jobSeed + ".stat.csv");
		// Originally, the following line was the line above.
		statFile = new File(statDir == null ? "." : statDir, "job." + 0 + ".stat.csv");
		if (statFile.exists()) {
			statFile.delete();
		}

		writeStatFileTitle();
	}

	public void writeStatFileTitle() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(statFile));
			writer.write("Gen,Time,ProgSizeMean,ProgSizeStd,FitMean,FitStd");
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToStatFile() {
		calcStatistics();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(statFile, true));
			writer.write(generation + "," + duration +
					"," + statisticsMap.get(POP_PROG_SIZE).getMean() +
					"," + statisticsMap.get(POP_PROG_SIZE).getStandardDeviation() +
					"," + statisticsMap.get(POP_FITNESS).getMean() +
					"," + statisticsMap.get(POP_FITNESS).getStandardDeviation()
			);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setupStatistics() {
		statisticsMap = new HashMap<>();
		statisticsMap.put(POP_PROG_SIZE, new DescriptiveStatistics());
		statisticsMap.put(POP_FITNESS, new DescriptiveStatistics());
	}

	public void calcStatistics() {
		statisticsMap.get(POP_PROG_SIZE).clear();
		statisticsMap.get(POP_FITNESS).clear();

		for (Individual indi : population.subpops[0].individuals) {
			int progSize = ((GPIndividual)indi).trees[0].child.numNodes(GPNode.NODESEARCH_ALL);
			statisticsMap.get(POP_PROG_SIZE).addValue(progSize);
			double fitness = indi.fitness.fitness();
			if(fitness != Double.POSITIVE_INFINITY && fitness != Double.NEGATIVE_INFINITY)
				statisticsMap.get(POP_FITNESS).addValue(fitness);
		}
	}

	/**
	 * Initialize the terminal set.
	 */
	public void initTerminalSets() {
		if (terminalFrom.equals("basic")) {
			terminalSets = new ArrayList<>();

			for (int i = 0; i < subpops; i++)
				terminalSets.add(UCARPPrimitiveSet.basicTerminalSet());
		}
		else if (terminalFrom.equals("extended")) {
			terminalSets = new ArrayList<>();

			for (int i = 0; i < subpops; i++)
				terminalSets.add(UCARPPrimitiveSet.extendedTerminalSet());
		}
		else if (terminalFrom.equals("seq")) {
			terminalSets = new ArrayList<>();

			for (int i = 0; i < subpops; i++)
				terminalSets.add(UCARPPrimitiveSet.seqTerminalSet());
		}
		else {
			initTerminalSetsFromCsv(new File(terminalFrom), UCARPPrimitiveSet.basicTerminalSet());
		}

		if (includeErc)
			for (int i = 0; i < subpops; i++)
				terminalSets.get(i).add(new DoubleERC());
	}

	/**
	 * Return the best individual of a particular subpopulation.
	 * @param subpop the subpopulation id.
	 * @return the best individual in that subpopulation.
	 */
	public Individual bestIndi(int subpop) {
		int best = 0;
		for(int x = 1; x < population.subpops[subpop].individuals.length; x++)
			if (population.subpops[subpop].individuals[x].fitness.betterThan(population.subpops[subpop].individuals[best].fitness))
				best = x;

		return population.subpops[subpop].individuals[best];
	}

	@Override
	public void setup(EvolutionState state, Parameter base) {
		super.setup(this, base);

		Parameter p;

		// get the job seed
		p = new Parameter("seed").push(""+0);
		jobSeed = parameters.getLongWithDefault(p, null, 0);

		// get the source of the terminal sets
 		p = new Parameter(P_TERMINALS_FROM);
 		terminalFrom = parameters.getStringWithDefault(p, null, "basic");

 		// get whether to include the double ERC in the terminal sets or not
		p = new Parameter(P_INCLUDE_ERC);
		includeErc = parameters.getBoolean(p, null, false);

		// get whether to rotate the evaluation model per generation or not
		p = new Parameter(P_ROTATE_EVAL_MODEL);
		rotateEvalModel = parameters.getBoolean(p, null, false);

		// get the number of subpopulations
		p = new Parameter(Initializer.P_POP).push(Population.P_SIZE);
		subpops = parameters.getInt(p,null,1);

		p = new Parameter("stat.file");
		String statFile = parameters.getString(p, null);
		if(statFile != null)
		{
			statFile = statFile.replaceFirst("\\$", "");
			Path statDirPath = Paths.get(statFile.replaceFirst("$", "")).getParent();
			statDir = statDirPath == null ? "." : statDirPath.toString();
		}

		initTerminalSets();

		p = new Parameter(P_CLEAR);
//		if(!parameters.contains(p))
//			output.fatal("Parameter not found: " + P_CLEAR);
		clear = parameters.getBoolean(p, null, false);
		output.warning("Clear: " + clear);

//		seenSituations = DBMaker.fileDB("file.db").fileMmapEnable().concurrencyDisable().make().hashMap("seen").keySerializer(Serializer.JAVA).valueSerializer(Serializer.JAVA).create();
//		seenSituations = DBMaker.memoryDB().concurrencyDisable().make().hashMap("seen").keySerializer(Serializer.JAVA).valueSerializer(Serializer.JAVA).create();
//		seenSituations = new HashMap<>();
	}

	protected void clear()
	{
		if(!clear || seenSituations.size() == 0)
		{
			seenSituations.clear();
			return;
		}

		int numDecisionSituations = 30;
		long shuffleSeed = 8295342;

		List<ReactiveDecisionSituation> allSeenSituations = getAllSeenSituations();
		Collections.shuffle(allSeenSituations, new Random(shuffleSeed));
		allSeenSituations = allSeenSituations.subList(0, numDecisionSituations);

		SimpleNichingAlgorithm.clearPopulation(this, allSeenSituations, 0, 1);
		allSeenSituations.clear();

		// Clear the list so that new situations do not pile on the old ones. Don't know if this is a good idea.
		seenSituations.clear();
	}

	public void updateSeenSituations(Individual ind, List<DecisionSituation> situations)
	{
		if(seenSituations.size() > 1)
			return;

		List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
		situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
		seenSituations.put(ind, clonedSituations);
		if(this.seenSituations.size() > 5)
		{
			seenSituations.remove(seenSituations.lastKey());
		}
	}

	public List<ReactiveDecisionSituation> getInitialSituations()
	{
		return initialSituations;
	}

	public void addToInitialSituations(DecisionSituation initialSituation)
	{
		if(this.initialSituations.size()> 10000)
			return;
		this.initialSituations.add((ReactiveDecisionSituation) initialSituation);
	}

	private List<ReactiveDecisionSituation> initialSituations = new ArrayList<>();

	List<ReactiveDecisionSituation> getAllSeenSituations()
	{
		ArrayList<ReactiveDecisionSituation> retval = new ArrayList<>();
		Set<Map.Entry<Individual, List<DecisionSituation>>> a = seenSituations.entrySet();
		for(Map.Entry<Individual, List<DecisionSituation>> e : a)
		{
			List<DecisionSituation> situations = e.getValue();
			for(DecisionSituation situation : situations)
			{
				ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
				if(rds.getPool().size() >= 2) // greater than two because the correlation-based phenotypics require it.
					retval.add(rds);
			}
		}

//		for(Individual ind : seenSituations.keySet())
//		{
//			List<DecisionSituation> situations = seenSituations.get(ind);
//			for(DecisionSituation situation : situations)
//			{
//				ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
//				if(rds.getPool().size() > 0)
//					retval.add(rds);
//			}
//		}

		return retval;
	}

	@Override
	public void run(int condition) {
		if (condition == C_STARTED_FRESH) {
			startFresh();
        }
		else {
			startFromCheckpoint();
        }

		initStatFile();
		setupStatistics();

		start = util.Timer.getCpuTime();

		int result = R_NOTDONE;
		while ( result == R_NOTDONE ) {
			result = evolve();
        }

		finish(result);
    }

	@Override
	public int evolve() {

	    if (generation > 0)
	        output.message("Generation " + generation);

	    // EVALUATION
	    statistics.preEvaluationStatistics(this);
	    evaluator.evaluatePopulation(this);
	    clear();
		statistics.postEvaluationStatistics(this);

		finish = util.Timer.getCpuTime();
		duration = 1.0 * (finish - start) / 1000000000;

		writeToStatFile();

		start = util.Timer.getCpuTime();

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

	boolean exchangePopulationPreBreeding()
	{
		// PRE-BREEDING EXCHANGING
		statistics.prePreBreedingExchangeStatistics(this);
		population = exchanger.preBreedingExchangePopulation(this);
		statistics.postPreBreedingExchangeStatistics(this);

		String exchangerWantsToShutdown = exchanger.runComplete(this);
		if (exchangerWantsToShutdown!=null)
		{
			output.message(exchangerWantsToShutdown);
			/*
			 * Don't really know what to return here.  The only place I could
			 * find where runComplete ever returns non-null is
			 * IslandExchange.  However, that can return non-null whether or
			 * not the ideal individual was found (for example, if there was
			 * a communication error with the server).
			 *
			 * Since the original version of this code didn't care, and the
			 * result was initialized to R_SUCCESS before the while loop, I'm
			 * just going to return R_SUCCESS here.
			 */

			return true;
		}

		return false;
	}

	void exchangePopulationPostBreeding()
	{
		statistics.prePostBreedingExchangeStatistics(this);
		population = exchanger.postBreedingExchangePopulation(this);
		statistics.postPostBreedingExchangeStatistics(this);
	}

	protected void breed()
	{
		statistics.preBreedingStatistics(this);

		population = breeder.breedPopulation(this);

		// POST-BREEDING EXCHANGING
		statistics.postBreedingStatistics(this);
	}

	void rotateEvalModel()
	{
		if (rotateEvalModel) {
			ReactiveGPHHProblem problem = (ReactiveGPHHProblem)evaluator.p_problem;
			problem.rotateEvaluationModel();
		}
	}

	void doCheckpoit()
	{
		if (checkpoint && generation%checkpointModulo == 0)
		{
			output.message("Checkpointing");
			statistics.preCheckpointStatistics(this);
			Checkpoint.setCheckpoint(this);
			statistics.postCheckpointStatistics(this);
		}
	}
}
