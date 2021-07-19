package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.TLGPIndividual;
import tl.gp.characterisation.RuleCharacterisation;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.sst.lsh.Vector;
import tl.knowledge.sst.lsh.families.EuclidianHashFamily;
import tl.knowledge.sst.lsh.families.HashFamily;
import tl.knowledge.surrogate.lsh.FittedVector;

import java.io.File;
import java.util.*;


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

    protected PoolFilter filter;

    protected RuleCharacterisation<int[]> trc;

    protected int dmsSize;

    private int breedingEntropyLogID;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);
        super.setup(state, base);

        int knowledgeSuccessLogID = setupLogger(state, base, true);

        Parameter p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

        dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
        log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

        trc = new TaskIndexCharacterisation(
                getInitialSituations().subList(0, Math.min(dmsSize, getInitialSituations().size())));

        int nSubPops = state.parameters.getInt(new Parameter("pop.subpops"), null);
        final int NUM_HASHES = 10;
        final int NUM_HASH_TABLES = 100;
        for(int i = 0; i < nSubPops; i++)
            history.add(setUpLSH(0, dmsSize, NUM_HASHES, NUM_HASH_TABLES));

        breedingEntropyLogID = setupLogger(this, new File(popLogPath, "pop/BreedingEntropy.csv").getAbsolutePath());
        log(this, breedingEntropyLogID, "Phase,");
        int i;
        for (i = 0; i < nSubPops - 1; i++)
        {
            log(this, breedingEntropyLogID, "SubPop" + i + ",");
        }
        log(this, breedingEntropyLogID, "SubPop" + i + "\n");
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

		/*
		There is a minor issue to consider here. Because the initializer is based on building GP trees, rather than GP
		individuals, there is no way to discriminate between transferred and discovered individuals. As a result, this
		method invocation will also add individuals that may have been transferred from a source domain. This is not a
		big issue because it just adds one minor duplicity but will not affect the original idea.
		*/
        updateSearchHistory(population);

        finish = util.Timer.getCpuTime();
        duration += 1.0 * (finish - start) / 1000000000;

        writeToStatFile();
        duration = 0;

        logPopulation(population);
        resetOrigin(population);

        logBreedingEntropy("before breeding");

        start = util.Timer.getCpuTime();

        // SHOULD WE QUIT?
        if (evaluator.runComplete(this) && quitOnRunComplete) {
            output.message("Found Ideal Individual");
            return R_SUCCESS;
        }
        // SHOULD WE QUIT?
        if (generation == numGenerations-1) return R_FAILURE;

        // This is where the immigrants are selected to be transferred.
        if (exchangePopulationPreBreeding()) return R_SUCCESS;

        // BREEDING
        breed();

        finish = util.Timer.getCpuTime();
        duration = (1.0 * (finish - start) / 1000000000);

        logBreedingEntropy("after breeding (before exchange)");

        start = util.Timer.getCpuTime();

        // POST-BREEDING EXCHANGING
        // This is where the selected immigrants from a source population will replace a set of individuals to die in
        // the target population.
        exchangePopulationPostBreeding();

        // Generate new instances if needed
        rotateEvalModel();

        // INCREMENT GENERATION AND CHECKPOINT
        generation++;
        doCheckpoit();

        return R_NOTDONE;
    }

    protected void logBreedingEntropy(String phase)
    {
        log(this, breedingEntropyLogID, phase + ",");
        int i;
        for (i = 0; i < this.population.subpops.length - 1; i++)
        {
            log(this, breedingEntropyLogID, entropy.entropy(population.subpops[i].individuals, 0, filter) + ",");
        }
        log(this, breedingEntropyLogID, entropy.entropy(population.subpops[i].individuals, 0, filter) + "\n");
    }

    protected void resetOrigin(Population pop)
    {
        for (int i = 0; i < pop.subpops.length; i++) {
            for (int j = 0; j < pop.subpops[i].individuals.length; j++) {
                Individual ind = pop.subpops[i].individuals[j];
                if(!(ind instanceof TLGPIndividual))
                    continue;
                TLGPIndividual tind = (TLGPIndividual)ind;
                tind.setOrigin("");
            }
        }
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

        List<Vector> query = database.query(new Vector(characterise), 0.0);
        return query.size();
    }

    List<HashSet<Integer>> hashedInds = null;
    int lastGenHSUpdated = -1;
    public boolean IsSeenInCurrentPop(int subpop, Individual ind)
    {
        if(lastGenHSUpdated != generation)
        {
            hashedInds = new ArrayList<>();
            for(int i = 0; i < population.subpops.length; i++)
            {
                HashSet<Integer> hs = new HashSet<>();
                for(int j = 0; j < population.subpops[i].individuals.length; j++)
                {
                    GPIndividual gind = (GPIndividual)population.subpops[i].individuals[j];
                    GPRoutingPolicy policy = new GPRoutingPolicy(filter, gind.trees[0]);
                    int[] ch = trc.characterise(policy);
                    int hcode = Arrays.hashCode(ch);
                    hs.add(hcode);
                }
                hashedInds.add(hs);
            }
            lastGenHSUpdated = generation;
        }

        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        int[] characterise = trc.characterise(policy);
        int hcode = Arrays.hashCode(characterise);

        return hashedInds.get(subpop).contains(hcode);
    }

    int lastGenHSImmUpdated = -1;
    List<HashSet<Integer>> hashedImmigrants = null;
    public boolean isSeenInTransferred(int subpop, Individual ind)
    {
        if(hashedImmigrants == null || hashedImmigrants.get(subpop) == null)
            return false;
        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        int[] characterise = trc.characterise(policy);
        int hcode = Arrays.hashCode(characterise);

        return hashedImmigrants.get(subpop).contains(hcode);
    }

    public void receiveImmigrant(int subpop, Individual ind, int index)
    {
        population.subpops[subpop].individuals[index] = ind;
        ind.evaluated = false;

        if(lastGenHSImmUpdated != generation)
        {
            lastGenHSImmUpdated = generation;
            hashedImmigrants = new ArrayList<>();
            for(int i = 0; i < population.subpops.length; i++)
                hashedImmigrants.add(new HashSet<>());
        }

        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        int[] characterise = trc.characterise(policy);
        int hcode = Arrays.hashCode(characterise);

        hashedImmigrants.get(subpop).add(hcode);
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
}
