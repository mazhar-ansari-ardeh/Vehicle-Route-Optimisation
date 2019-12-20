package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import gphhucarp.gp.GPHHEvolutionState;
import gphhucarp.gp.ReactiveGPHHProblem;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import tl.TLLogger;
import tl.gphhucarp.UCARPUtils;
import tl.knowledge.ppt.gp.PPTBreedingPipeline;
import tl.knowledge.ppt.pipe.FrequencyLearner;
import tl.knowledge.ppt.pipe.PPTree;

import java.util.Arrays;

/**
 * The evolution state of evolving routing policy with GPHH.
 *
 * @author gphhucarp
 *
 */
public class PPTEvolutionState extends GPHHEvolutionState implements TLLogger<GPNode>
{
//    /**
//     * Statistics to store.
//     */
//    public static final String POP_PROG_SIZE = "pop-prog-size";
//    public static final String POP_FITNESS = "pop-fitness";
//
//    /**
//     * Read the file to specify the terminals.
//     */
//    public static final String P_TERMINALS_FROM = "terminals-from";
//    public static final String P_INCLUDE_ERC = "include-erc";
//
//    /**
//     * Whether to rotate the evaluation model or not.
//     */
//    public static final String P_ROTATE_EVAL_MODEL = "rotate-eval-model";
//
//    protected String terminalFrom;
//    protected boolean includeErc;
//    protected boolean rotateEvalModel;
//
//    protected long jobSeed;
//
//    protected Map<String, DescriptiveStatistics> statisticsMap;
//    protected File statFile;
//    protected String statDir;
//
//    public Map<String, DescriptiveStatistics> getStatisticsMap() {
//        return statisticsMap;
//    }
//
//    public DescriptiveStatistics getStatistics(String key) {
//        return statisticsMap.get(key);
//    }
//
//    public long getJobSeed() {
//        return jobSeed;
//    }
//
//    protected long start, finish;
//    protected double duration;

    private PPTree ppt;

//    /**
//     * The size of the set that is sampled from the population set to learn from for the frequency-based learning
//     * method.
//     */
//    public static final String P_SAMPLE_SIZE = "sample-size";
//
//    /**
//     * The tournament size that is used by the frequency-based learning method.
//     */
//    public final static String P_TOURNAMENT_SIZE = "tournament-size";
//
//    /**
//     * The learning rate. The meaning of this parameter may be different for different learning algorithms.
//     */
//    public static final String P_LEARNING_RATE = "lr";
//
//
//    private FrequencyLearner setupFrequencyLearner(EvolutionState state, Parameter base, String[] functions, String[] terminals)
//    {
//        Parameter p = base.push(P_SAMPLE_SIZE);
//        int sampleSize = state.parameters.getInt(p, null);
//        if(sampleSize <= 0)
//            state.output.fatal("Sample size must be a positive value: " + sampleSize);
//        else
//            state.output.warning("Sample size: " + sampleSize);
//
//        p = base.push(P_TOURNAMENT_SIZE);
//
//        // The tournament size to sample individuals from the population to learn.
//        int tournamentSize = state.parameters.getInt(p, null);
//        if(tournamentSize > sampleSize)
//            state.output.fatal("Tournament size must be positive and smaller than sample size: " + tournamentSize);
//        else
//            state.output.warning("Tournament size: " + tournamentSize);
//
//        p = base.push(P_LEARNING_RATE);
//
//        // The learning rate for learning.
//        double lr = state.parameters.getDouble(p, null);
//        if(lr <= 0 || lr > 1)
//            state.output.fatal("The value of the learning rate is invalid:" + lr);
//        else
//            state.output.warning("Learning rate: " + lr);
//
//        return new FrequencyLearner(state, 0, functions, terminals, lr, sampleSize, tournamentSize);
//    }

    /**
     * The learner that will be used to learn the PPT from the population.
     */
    private FrequencyLearner learner;

    public final String P_BASE = "ppt-state";

    public static final String P_PPT_STAT_LOG = "ppt-stat-log";
    public static final String P_PPT_LOG = "ppt-log";

    private int pptStatLogID;
    private int pptLogID;

    public PPTree getPpt()
    {
        return ppt;
    }

    public void setPpt(PPTree ppt)
    {
        this.ppt = ppt;
    }

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        Parameter pptBase = new Parameter(P_BASE);

        String[] terminals = UCARPUtils.getTerminalNames();
        String[] functions = UCARPUtils.getFunctionSet();
        learner = FrequencyLearner.newFrequencyLearner(state, pptBase, functions, terminals);

        pptStatLogID = setupLogger(state, pptBase, P_PPT_STAT_LOG);
        log(this, pptStatLogID, "Generation, PPT Ind Count, average, std, min, max, Non-PPT Ind Count, average, std, min, max\n");
        pptLogID = setupLogger(state, pptBase, P_PPT_LOG);
    }

//
//    public void initStatFile() {
////		statFile = new File(statDir == null ? "." : statDir, "job." + jobSeed + ".stat.csv");
//        // Originally, the following line was the line above.
//        statFile = new File(statDir == null ? "." : statDir, "job." + 0 + ".stat.csv");
//        if (statFile.exists()) {
//            statFile.delete();
//        }
//
//        writeStatFileTitle();
//    }
//
//    public void writeStatFileTitle() {
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(statFile));
//            writer.write("Gen,Time,ProgSizeMean,ProgSizeStd,FitMean,FitStd");
//            writer.newLine();
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void writeToStatFile() {
//        calcStatistics();
//
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(statFile, true));
//            writer.write(generation + "," + duration +
//                    "," + statisticsMap.get(POP_PROG_SIZE).getMean() +
//                    "," + statisticsMap.get(POP_PROG_SIZE).getStandardDeviation() +
//                    "," + statisticsMap.get(POP_FITNESS).getMean() +
//                    "," + statisticsMap.get(POP_FITNESS).getStandardDeviation()
//            );
//            writer.newLine();
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void setupStatistics() {
//        statisticsMap = new HashMap<>();
//        statisticsMap.put(POP_PROG_SIZE, new DescriptiveStatistics());
//        statisticsMap.put(POP_FITNESS, new DescriptiveStatistics());
//    }
//
//    public void calcStatistics() {
//        statisticsMap.get(POP_PROG_SIZE).clear();
//        statisticsMap.get(POP_FITNESS).clear();
//
//        for (Individual indi : population.subpops[0].individuals) {
//            int progSize = ((GPIndividual)indi).trees[0].child.numNodes(GPNode.NODESEARCH_ALL);
//            statisticsMap.get(POP_PROG_SIZE).addValue(progSize);
//            double fitness = indi.fitness.fitness();
//            statisticsMap.get(POP_FITNESS).addValue(fitness);
//        }
//    }
//
//    /**
//     * Initialize the terminal set.
//     */
//    public void initTerminalSets() {
//        if (terminalFrom.equals("basic")) {
//            terminalSets = new ArrayList<>();
//
//            for (int i = 0; i < subpops; i++)
//                terminalSets.add(UCARPPrimitiveSet.basicTerminalSet());
//        }
//        else if (terminalFrom.equals("extended")) {
//            terminalSets = new ArrayList<>();
//
//            for (int i = 0; i < subpops; i++)
//                terminalSets.add(UCARPPrimitiveSet.extendedTerminalSet());
//        }
//        else if (terminalFrom.equals("seq")) {
//            terminalSets = new ArrayList<>();
//
//            for (int i = 0; i < subpops; i++)
//                terminalSets.add(UCARPPrimitiveSet.seqTerminalSet());
//        }
//        else {
//            initTerminalSetsFromCsv(new File(terminalFrom), UCARPPrimitiveSet.basicTerminalSet());
//        }
//
//        if (includeErc)
//            for (int i = 0; i < subpops; i++)
//                terminalSets.get(i).add(new DoubleERC());
//    }
//
//    /**
//     * Return the best individual of a particular subpopulation.
//     * @param subpop the subpopulation id.
//     * @return the best individual in that subpopulation.
//     */
//    public Individual bestIndi(int subpop) {
//        int best = 0;
//        for(int x = 1; x < population.subpops[subpop].individuals.length; x++)
//            if (population.subpops[subpop].individuals[x].fitness.betterThan(population.subpops[subpop].individuals[best].fitness))
//                best = x;
//
//        return population.subpops[subpop].individuals[best];
//    }
//
//    @Override
//    public void setup(EvolutionState state, Parameter base) {
//        super.setup(this, base);
//
//        Parameter p;
//
//        // get the job seed
//        p = new Parameter("seed").push(""+0);
//        jobSeed = parameters.getLongWithDefault(p, null, 0);
//
//        // get the source of the terminal sets
//        p = new Parameter(P_TERMINALS_FROM);
//        terminalFrom = parameters.getStringWithDefault(p, null, "basic");
//
//        // get whether to include the double ERC in the terminal sets or not
//        p = new Parameter(P_INCLUDE_ERC);
//        includeErc = parameters.getBoolean(p, null, false);
//
//        // get whether to rotate the evaluation model per generation or not
//        p = new Parameter(P_ROTATE_EVAL_MODEL);
//        rotateEvalModel = parameters.getBoolean(p, null, false);
//
//        // get the number of subpopulations
//        p = new Parameter(Initializer.P_POP).push(Population.P_SIZE);
//        subpops = parameters.getInt(p,null,1);
//
//        p = new Parameter("stat.file");
//        String statFile = parameters.getString(p, null);
//        if(statFile != null)
//        {
//            statFile = statFile.replaceFirst("\\$", "");
//            Path statDirPath = Paths.get(statFile.replaceFirst("$", "")).getParent();
//            statDir = statDirPath == null ? "." : statDirPath.toString();
//        }
//
//        initTerminalSets();
//    }
//
//    @Override
//    public void run(int condition) {
//        if (condition == C_STARTED_FRESH) {
//            startFresh();
//        }
//        else {
//            startFromCheckpoint();
//        }
//
//        initStatFile();
//        setupStatistics();
//
//        start = util.Timer.getCpuTime();
//
//        int result = R_NOTDONE;
//        while ( result == R_NOTDONE ) {
//            result = evolve();
//        }
//
//        finish(result);
//    }
//


    void logPPTStat()
    {
        SummaryStatistics pptSS = new SummaryStatistics();
        SummaryStatistics nonPPTSS = new SummaryStatistics();

//      log(this, pptStatLogID, "---------------------------------------------------------\n");

        for(int i = 0; i < population.subpops[0].individuals.length; i++)
        {
            TLGPIndividual ind = (TLGPIndividual) population.subpops[0].individuals[i];
            String origin = ind.getOrigin();
            if(origin != null && origin.toLowerCase().equals(PPTBreedingPipeline.ORIGIN))
            {
                pptSS.addValue(ind.fitness.fitness());
            }
            else
            {
                nonPPTSS.addValue(ind.fitness.fitness());
            }
        }

//        log(this, pptStatLogID, "PPT stats:\n");
        log(this, pptStatLogID, ""   + generation );
        log(this, pptStatLogID, ", " +  pptSS.getN() );
        log(this, pptStatLogID, ", " + pptSS.getMean());
        log(this, pptStatLogID, ", " + pptSS.getStandardDeviation() );
        log(this, pptStatLogID, ", " + pptSS.getMin());
        log(this, pptStatLogID, ", " + pptSS.getMax());

//        log(this, pptStatLogID, "Non-PPT stats:\n");
        log(this, pptStatLogID, ", " + nonPPTSS.getN());
        log(this, pptStatLogID, ", " + nonPPTSS.getMean() );
        log(this, pptStatLogID, ", " + nonPPTSS.getStandardDeviation() );
        log(this, pptStatLogID, ", " + nonPPTSS.getMin() );
        log(this, pptStatLogID, ", " + nonPPTSS.getMax() + "\n");
//        log(this, pptStatLogID, "---------------------------------------------------------\n\n");
    }

    /**
     * The crossover and mutation operators do not clone individuals and perform their operations on the individual itself.
     * As a result, if an individual is originated from the PPT first and then is gone through them, the origin will still be
     * PPT but when it is changed, it is a new individual and should not have the same origin. This is done here because I
     * don't intend to modify those operators.
     *
     * Invoke this before breeding.
     */
    private void resetPopulationOrigin()
    {
        Individual[] inds = population.subpops[0].individuals;
        for (Individual ind : inds)
        {
            ((TLGPIndividual) ind).setOrigin("");
        }
    }

    @Override
    public int evolve() {

        if (generation > 0)
            output.message("Generation " + generation);

        // EVALUATION
        statistics.preEvaluationStatistics(this);
        evaluator.evaluatePopulation(this);
        statistics.postEvaluationStatistics(this);


        logPPTStat();

        GPIndividual[] inds = new GPIndividual[population.subpops[0].individuals.length];
        inds = Arrays.copyOf(population.subpops[0].individuals, inds.length, GPIndividual[].class);
        learner.adaptTowards(ppt, inds, 0);

        log(this, pptLogID, "Gen:\t" + generation + ":\n");
        log(this, pptLogID, "---------------------------------------------------------\n");
        log(this, pptLogID, "PPT after adaptation: " + ppt.toGVString(2) + "\n");
        log(this, pptLogID, "---------------------------------------------------------\n\n");

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
        if (generation == numGenerations-1) {
            return R_FAILURE;
        }

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

            return R_SUCCESS;
        }

        // BREEDING
        statistics.preBreedingStatistics(this);

        resetPopulationOrigin();
        population = breeder.breedPopulation(this);

        // POST-BREEDING EXCHANGING
        statistics.postBreedingStatistics(this);

        // POST-BREEDING EXCHANGING
        statistics.prePostBreedingExchangeStatistics(this);
        population = exchanger.postBreedingExchangePopulation(this);
        statistics.postPostBreedingExchangeStatistics(this);

        // Generate new instances if needed
        if (rotateEvalModel) {
            ReactiveGPHHProblem problem = (ReactiveGPHHProblem)evaluator.p_problem;
            problem.rotateEvaluationModel();
        }

        // INCREMENT GENERATION AND CHECKPOINT
        generation++;
        if (checkpoint && generation%checkpointModulo == 0)
        {
            output.message("Checkpointing");
            statistics.preCheckpointStatistics(this);
            Checkpoint.setCheckpoint(this);
            statistics.postCheckpointStatistics(this);
        }

        return R_NOTDONE;
    }
}
