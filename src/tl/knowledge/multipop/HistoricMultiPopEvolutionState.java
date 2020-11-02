package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.TLGPIndividual;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.sst.lsh.Vector;
import tl.knowledge.sst.lsh.families.EuclidianHashFamily;
import tl.knowledge.sst.lsh.families.HashFamily;
import tl.knowledge.surrogate.lsh.FittedVector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * This class implements a multi-population evolution state that also keeps track of the individuals that it has evolved
 * for each population during its course of evolution.
 */
public class HistoricMultiPopEvolutionState extends MultiPopEvolutionState
{
    public static final String P_BASE = "hmt-state";

    /**
     * Keeps a history of seen individuals for each sub-population.
     */
    protected ArrayList<LSH> history = new ArrayList<>();

    /**
     * Size of the decision-making situations.
     */
    public final String P_DMS_SIZE = "dms-size";

    /**
     * The directory to which the population is logged at the end of each generation.
     */
    public static final String P_POP_LOG_PATH = "pop-log-path";
    private String popLogPath;

    PoolFilter filter;

    TaskIndexCharacterisation trc;

    protected int dmsSize;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        base = new Parameter(P_BASE);

        int knowledgeSuccessLogID = setupLogger(state, base, true);

        Parameter p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

        dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
        log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

        trc = new TaskIndexCharacterisation(
                getInitialSituations().subList(0, Math.min(dmsSize, getInitialSituations().size())));

        popLogPath = state.parameters.getString(base.push(P_POP_LOG_PATH), null);
        if(popLogPath == null)
        {
            state.output.fatal("Population log path cannot be null");
            return;
        }
        state.output.warning("Population log path: " + popLogPath);

        int nSubPops = state.parameters.getInt(new Parameter("pop.subpops"), null);
        final int NUM_HASHES = 10;
        final int NUM_HASH_TABLES = 100;
        for(int i = 0; i < nSubPops; i++)
            history.add(setUpLSH(0, dmsSize, NUM_HASHES, NUM_HASH_TABLES));
    }

    private LSH setUpLSH(int radius, int dimensions, int numberOfHashes, int numberOfHashTables)
    {
        final int W = 10;
        int w = W * radius;
        w = w == 0 ? 1 : w;
        HashFamily family = new EuclidianHashFamily(w, dimensions);
        LSH lsh = new LSH(family);
        lsh.buildIndex(numberOfHashes, numberOfHashTables, this.random[0]);

        return lsh;
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

//        /* Newly discovered individuals are inside the population so there is no need to for them any more. Also, since
//         * the updateHistory method uses the isSeen method, which in turn reviews the tempInds, this archive should be
//         * emptied.
//         */
//        tempInds.clear();
		/*
		There is a minor issue to consider here. Because the initializer is based on building GP trees, rather than GP
		individuals, there is no way to discriminate between transferred and discovered individuals. As a result, this
		method invocation will also add individuals that may have been transferred from a source domain. This is not a
		big issue because it just adds one minor duplicity but will not affect the original idea.
		*/
        updateSearchHistory(population);

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


    protected void updateSearchHistory(Population population)
    {
        for(int subpop = 0; subpop < population.subpops.length; subpop++)
            for (Individual ind : population.subpops[subpop].individuals)
            {
//                SSTIndividual i = (SSTIndividual) ind;
                updateLSH(history.get(subpop), ind);
            }
    }

    public int isSeenIn(int subpop, Individual i)
    {
        LSH database = history.get(subpop);

        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0]);
        int[] characterise = trc.characterise(policy);

        List<Vector> query = database.query(new Vector(characterise), 10);
        return query.size();
    }

    public int[] characterise(Individual ind)
    {
        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual) ind).trees[0]);
        return trc.characterise(policy);
    }

    private void updateLSH(LSH lsh, Individual ind)
    {
        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual) ind).trees[0]);
        int[] characterise = trc.characterise(policy);

        lsh.add(new FittedVector(characterise, ind.fitness.fitness()));
    }

    private void logPopulation(Population population)
    {
        assert population != null;
        Individual[] pop = population.subpops[0].individuals;
        assert pop != null;

        int popLogID = setupLogger(this, new File(popLogPath, "pop/Pop." + generation + ".csv").getAbsolutePath());
        log(this, popLogID, "Origin,Fitness,Tree\n");
        Arrays.stream(pop).map(i -> (TLGPIndividual)i).sorted(Comparator.comparingDouble(j -> j.fitness.fitness())).forEach(
                i -> log(this, popLogID,
                        i.getOrigin() + "," + i.fitness.fitness() + "," + i.trees[0].child.makeLispTree() + "\n")
        );
        closeLogger(this, popLogID);
    }
}
