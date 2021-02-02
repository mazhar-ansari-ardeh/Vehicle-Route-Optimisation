package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This is an extension of the {@link MultiPopEvolutionState} class that
 * performs a clearing on each sub-population.
 */
public class ClearingMultiPopEvolutionState extends MultiPopEvolutionState
{
    public static final String P_BASE = "clear-multipop-state";
    /**
     * The number of duplicates to be cleared from each sub-population.
     */
    public static final String P_NUM_CLEAR = "num-clear";
    private int numClear;

    /**
     * If {@code true}, the individuals will be sorted in reverse order in terms of their fitness.
     */
    public static final String P_REVERSE_SORT = "reverse-sort";
    private boolean reverseSort = true;

    /**
     * Size of the decision-making situations.
     */
    public final String P_DMS_SIZE = "dms-size";
    protected int dmsSize;

    SituationBasedTreeSimilarityMetric metric;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);
        super.setup(state, base);

        numClear = state.parameters.getInt(base.push(P_NUM_CLEAR), null);
        if(numClear < 0)
            state.output.fatal("Invalid number of duplicate clearing: " + numClear);
        state.output.warning("Number of duplicate clearing: " + numClear + "\n");

        dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
        state.output.warning("DMS size: " + dmsSize + "\n");

        reverseSort = state.parameters.getBoolean(base.push(P_REVERSE_SORT), null, true);
        state.output.warning("Reverse sort: " + reverseSort + "\n");

        List<ReactiveDecisionSituation> situations = getInitialSituations().subList(0,
                Math.min(getInitialSituations().size(), dmsSize));

        metric = new HammingPhenoTreeSimilarityMetric();
        metric.setSituations(situations);
        setDMSSavingEnabled(false);
    }

    @Override
    public void updateSeenSituations(int subPop, Individual ind, List<DecisionSituation> situations) {
        super.updateSeenSituations(subPop, ind, situations);
    }

    private void clear()
    {
        for(int subpop = 0; subpop < population.subpops.length; subpop++)
        {
            Individual[] individuals = population.subpops[subpop].individuals;
//            List<Individual> inds = Arrays.asList(population.subpops[subpop].individuals);
            // Reverse order because DuplicateSelection does so.
//            inds.sort(Collections.reverseOrder(Comparator.comparingDouble(o -> o.fitness.fitness())));
            if(reverseSort)
                Arrays.sort(individuals, Collections.reverseOrder(Comparator.comparingDouble(o -> o.fitness.fitness())));
            else
                Arrays.sort(individuals, Comparator.comparingDouble(o -> o.fitness.fitness()));
            SimpleNichingAlgorithm.clearPopulation(individuals, filter, metric, 0.0, 1, numClear);
        }
    }

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
        logPopulation(population);

        start = util.Timer.getCpuTime();

        // SHOULD WE QUIT?
        if (evaluator.runComplete(this) && quitOnRunComplete) {
            output.message("Found Ideal Individual");
            return R_SUCCESS;
        }

        // SHOULD WE QUIT?
        if (generation == numGenerations-1) return R_FAILURE;

        // PRE-BREEDING EXCHANGE
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

}
