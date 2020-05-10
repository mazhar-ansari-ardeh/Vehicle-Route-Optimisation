package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Checkpoint;
import ec.util.Parameter;
import gphhucarp.gp.ReactiveGPHHProblem;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import tl.TLLogger;
import tl.gphhucarp.UCARPUtils;
import tl.gphhucarp.dms.DMSSavingGPHHState;
import tl.knowledge.ppt.gp.PPTBreedingPipeline;
import tl.knowledge.ppt.pipe.FrequencyLearner;
import tl.knowledge.ppt.pipe.PPTree;

import java.util.*;

/**
 * The evolution state of evolving routing policy with GPHH.
 *
 * @author gphhucarp
 *
 */
public class PPTEvolutionState extends DMSSavingGPHHState implements TLLogger<GPNode>
{
    private PPTree ppt;

    /**
     * The learner that will be used to learn the PPT from the population.
     */
    private FrequencyLearner learner;

    public final String P_BASE = "ppt-state";

    public static final String P_PPT_STAT_LOG = "ppt-stat-log";
    public static final String P_PPT_LOG = "ppt-log";

    private int pptStatLogID;
    private int pptLogID;
    private int populationLogId;

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
        log(this, pptStatLogID, "Generation, PPT Ind Count, average, std, min, max, CompPPT Ind Count, average, std, min, max, Non-PPT Ind Count, average, std, min, max\n");
        pptLogID = setupLogger(state, pptBase, P_PPT_LOG);
    }

//    void logPPTStat2()
//    {
//        if(generation == 0)
//            return;
//        HashMap<String, SummaryStatistics> stats = new HashMap<>();
//        for(int i = 0; i < population.subpops[0].individuals.length; i++)
//        {
//            TLGPIndividual ind = (TLGPIndividual) population.subpops[0].individuals[i];
//            String origin = ind.getOrigin();
//            double indFit = ind.fitness.fitness();
//            if(!stats.containsKey(origin))
//                stats.put(origin, new SummaryStatistics());
//            SummaryStatistics st = stats.get(origin);
//            st.addValue(indFit);
//        }
//
//        if(!statHeaderWritten)
//        {
//            log(this, pptStatLogID, "Generation");
//            for(String org : stats.keySet())
//            {
//                log(this, pptStatLogID, ", " + org + ", " + "Ind Count, average, std, min, max");
//            }
//            log(this, pptStatLogID, "\n");
//            statHeaderWritten = true;
//        }
//
//
//        log(this, pptStatLogID, ""   + generation );
//        for (String org : stats.keySet())
//        {
//            SummaryStatistics st = stats.get(org);
//            log(this, pptStatLogID, ", " + st.getN() );
//            log(this, pptStatLogID, ", " + st.getMean());
//            log(this, pptStatLogID, ", " + st.getStandardDeviation() );
//            log(this, pptStatLogID, ", " + st.getMin());
//            log(this, pptStatLogID, ", " + st.getMax());
////            ppt.setFitness(pptSS.getMean()); // TODO: Do something about this.
//        }
//        log(this, pptStatLogID, "\n");
//    }

    void logPPTStat()
    {
        SummaryStatistics pptSS = new SummaryStatistics();
        SummaryStatistics cmpPPTSS = new SummaryStatistics();
        SummaryStatistics nonPPTSS = new SummaryStatistics();

        double bestPPTFit = Double.MAX_VALUE;
        int bestPPTInd = 0;
        double bestCompPPTFit = Double.MAX_VALUE;
        int bestCompPPTInd = 0;
        double bestNonPPTFit = Double.MAX_VALUE;
        int bestNonPPTInd = 0;

        for(int i = 0; i < population.subpops[0].individuals.length; i++)
        {
            TLGPIndividual ind = (TLGPIndividual) population.subpops[0].individuals[i];
            String origin = ind.getOrigin();
            double indFit = ind.fitness.fitness();
            if(indFit == Double.POSITIVE_INFINITY)
                continue;
            if(origin != null && origin.toLowerCase().equals(PPTBreedingPipeline.ORIGIN))
            {
                pptSS.addValue(indFit);
                if(indFit < bestPPTFit)
                {
                    bestPPTFit = indFit;
                    bestPPTInd = i;
                }
            }
            else if(origin != null && origin.toLowerCase().equals(PPTBreedingPipeline.CMP_ORIGIN))
            {
                cmpPPTSS.addValue(indFit);
                if(indFit < bestCompPPTFit)
                {
                    bestCompPPTFit = indFit;
                    bestCompPPTInd = i;
                }
            }
            else
            {
                nonPPTSS.addValue(indFit);
                if(indFit < bestNonPPTFit)
                {
                    bestNonPPTFit = indFit;
                    bestNonPPTInd = i;
                }
            }
        }

        log(this, pptStatLogID, ""   + generation );
        log(this, pptStatLogID, ", " + pptSS.getN() );
        log(this, pptStatLogID, ", " + pptSS.getMean());
        log(this, pptStatLogID, ", " + pptSS.getStandardDeviation() );
        log(this, pptStatLogID, ", " + pptSS.getMin());
        log(this, pptStatLogID, ", " + pptSS.getMax());
        ppt.setFitness(pptSS.getMean());

        log(this, pptStatLogID, ", " + cmpPPTSS.getN() );
        log(this, pptStatLogID, ", " + cmpPPTSS.getMean());
        log(this, pptStatLogID, ", " + cmpPPTSS.getStandardDeviation() );
        log(this, pptStatLogID, ", " + cmpPPTSS.getMin());
        log(this, pptStatLogID, ", " + cmpPPTSS.getMax());

        log(this, pptStatLogID, ", " + nonPPTSS.getN());
        log(this, pptStatLogID, ", " + nonPPTSS.getMean() );
        log(this, pptStatLogID, ", " + nonPPTSS.getStandardDeviation() );
        log(this, pptStatLogID, ", " + nonPPTSS.getMin() );
        log(this, pptStatLogID, ", " + nonPPTSS.getMax() + "\n");

        log(this, pptLogID, "Gen:" + generation + ":\n");
        log(this, pptLogID, "---------------------------------------------------------\n");
        log(this, pptLogID, "PPT after adaptation: \n" + ppt.toGVString(2) + "\n\n");
        TLGPIndividual ind = ((TLGPIndividual) population.subpops[0].individuals[bestPPTInd]);
        log(this, pptLogID, "Best PPT individual, fitness=" + ind.fitness.fitness() + ": \n"  + ind.trees[0].child.makeGraphvizTree() + "\n\n");
        ind = ((TLGPIndividual) population.subpops[0].individuals[bestCompPPTInd]);
        log(this, pptLogID, "Best CompPPT individual, fitness=" + ind.fitness.fitness() + ": \n" + ind.trees[0].child.makeGraphvizTree() + "\n\n");
        ind = ((TLGPIndividual) population.subpops[0].individuals[bestNonPPTInd]);
        log(this, pptLogID, "Best NonPPT individual, fitness=" + ind.fitness.fitness() + ": \n" + ind.trees[0].child.makeGraphvizTree() + "\n\n");
        log(this, pptLogID, "---------------------------------------------------------\n\n");
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

    void logPopulation(GPIndividual[] inds)
    {
        if(populationLogId < 0)
            populationLogId = setupLogger(this, "population.txt");
        log(this, populationLogId, "Gen: " + generation + "\n");
        for(GPIndividual ind : inds)
        {
            log(this, populationLogId, "Fit: " + ind.fitness.fitness() + "\n");
            log(this, populationLogId, ind.trees[0].child.makeLispTree() + "\n\n");
        }
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


        GPIndividual[] inds = new GPIndividual[population.subpops[0].individuals.length];
        inds = Arrays.copyOf(population.subpops[0].individuals, inds.length, GPIndividual[].class);
        learner.adaptTowards(ppt, inds, 0);

        logPPTStat();

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
