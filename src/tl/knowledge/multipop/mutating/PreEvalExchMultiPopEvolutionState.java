package tl.knowledge.multipop.mutating;

import ec.EvolutionState;
import ec.simple.SimpleBreeder;
import ec.util.Parameter;
import tl.knowledge.multipop.MultiPopEvolutionState;

public class PreEvalExchMultiPopEvolutionState extends MultiPopEvolutionState
{
    public static final String P_NUM_EVALUATIONS = "num-evaluations";
    private int maxNumEvals;

    private int numEvaluated = 0;

    public static final String P_BASE = "preval-mt-state";

    private int populationSize;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);
        super.setup(state, base);

        int nsubpop = state.parameters.getInt(new Parameter("pop.subpops"), null);
        SimpleBreeder eval = (SimpleBreeder) breeder;
        for(int i = 0; i < nsubpop; i++)
        {
            // Set the minimum population size to the actual population size and set the reduceBy to a high value
            // (just to be sure). This way, the breeder reduces the population back to its original size.
            int subpopsize = state.parameters.getInt(new Parameter("pop.subpop." + i + ".size"), null);
            eval.minimumSize[i] = subpopsize;
            eval.reduceBy[i] = Integer.MAX_VALUE;
        }

        maxNumEvals = parameters.getInt(base.push(P_NUM_EVALUATIONS), null);
        output.warning(String.format("Evaluation budget: %d", maxNumEvals));

        populationSize = parameters.getInt(new Parameter("pop.subpop.0.size"), null);
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

        if (exchangePopulationPreBreeding()) return R_SUCCESS;

        exchangePopulationPostBreeding();

        ((RangeBasedSimpleEvaluator)evaluator).evaluatePopulation(this, populationSize);
        for(int i = 0; i < population.subpops.length; i++)
            numEvaluated += population.subpops[i].individuals.length;

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
        if (numEvaluated >= maxNumEvals) return R_FAILURE;

        // BREEDING
        breed();

        // Generate new instances if needed
        rotateEvalModel();

        // INCREMENT GENERATION AND CHECKPOINT
        generation++;
        doCheckpoit();

        return R_NOTDONE;
    }
}
