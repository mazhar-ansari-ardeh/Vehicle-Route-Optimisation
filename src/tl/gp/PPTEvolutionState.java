package tl.gp;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import gphhucarp.gp.GPHHEvolutionState;
import gphhucarp.gp.ReactiveGPHHProblem;
import tl.TLLogger;
import tl.gphhucarp.UCARPUtils;
import tl.knowledge.ppt.pipe.FrequencyLearner;
import tl.knowledge.ppt.pipe.PPTree;

import java.util.Arrays;

public class PPTEvolutionState extends GPHHEvolutionState implements TLLogger<GPNode>
{

    private FrequencyLearner learner;
    private PPTree tree;

    public static final String P_LEARNING_RATE = "lr";

    public static final String P_SAMPLE_SIZE = "sample-size";

    public final static String P_TOURNAMENT_SIZE = "tournament-size";

    private int knowledgeSuccessLogID;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        base = new Parameter("ppt-state");

        knowledgeSuccessLogID = setupLogger(state, base);

        Parameter p = base.push(P_LEARNING_RATE);

        // The learning rate for learning.
        double lr = state.parameters.getDouble(p, null);
        if(lr <= 0 || lr > 1)
            state.output.fatal("The value of the learning rate is invalid:" + lr);
        else
            state.output.warning("Learning rate: " + lr);

        p = base.push(P_SAMPLE_SIZE);

        // The size of the set that is sampled from the population set to learn from.
        int sampleSize = state.parameters.getInt(p, null);
        if(sampleSize <= 0)
            state.output.fatal("Sample size must be a positive value: " + sampleSize);
        else
            state.output.warning("Sample size: " + sampleSize);

        p = base.push(P_TOURNAMENT_SIZE);

         // The tournament size to sample individuals from the population to learn.
        int tournamentSize = state.parameters.getInt(p, null);
        if(tournamentSize <= 0 || tournamentSize > sampleSize)
            state.output.fatal("Tournament size must be positive and smaller than sample size: " + tournamentSize);
        else
            state.output.warning("Tournament size: " + tournamentSize);


        String[] terminals = UCARPUtils.getTerminalNames();
        String[] functions = UCARPUtils.getFunctionSet();
        learner = new FrequencyLearner(state, 0, functions, terminals, lr, sampleSize, tournamentSize);

        tree = new PPTree(learner, functions, terminals);
    }

    @Override
    public int evolve() {

        if (generation > 0)
            output.message("Generation " + generation);

        // EVALUATION
        statistics.preEvaluationStatistics(this);
        evaluator.evaluatePopulation(this);
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

        {
            GPIndividual[] pop = Arrays.copyOf(population.subpops[0].individuals, population.subpops[0].individuals.length, GPIndividual[].class);
            learner.adaptTowards(tree, pop, 0);
            log(this, knowledgeSuccessLogID, "PPT at generation " + generation + ": " + tree.toString());
            for(int i = 0; i < population.subpops[0].individuals.length; i++)
                population.subpops[0].individuals[i] = GPIndividualUtils.asGPIndividual(
                        tree.sampleIndividual(this.random[0]), population.subpops[0].individuals[0].fitness);
        }

//        population = breeder.breedPopulation(this);

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
