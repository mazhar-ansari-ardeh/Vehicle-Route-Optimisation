package gphhucarp.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tl.TLLogger;
import tl.gp.PopulationUtils;
import tl.gp.characterisation.RuleCharacterisation;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.gp.similarity.RefRulePhenoTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.gphhucarp.dms.DMSSavingGPHHState;
import tl.knowledge.surrogate.Surrogate;
import tl.knowledge.surrogate.knn.FILOPhenotypicUpdatePolicy;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;

import java.io.File;
import java.util.*;

/**
 * This class implements a surrogate-based GP in which after each generation, two populations are created from the
 * evaluated population. Then, a number of the duplicates in the first population are replaced with the same number of
 * unique individuals from the second population. The second population is evaluated with a surrogate model that is
 * updated after each generation.
 */
public class PartiallySurrogatedGPHHState extends DMSSavingGPHHState implements TLLogger<GPNode>
{
    public Surrogate surFitness;

    private int surLogID;
    private int interimPopLogID;
    private int popLogID;

    private String surLogPath;

    private static final String P_BASE = "part-surr-state";

    private static final String P_SURR_LOG_PATH = "surr-log-path";

    public static final String P_NUM_IMMIGRANTS = "num-immigrants";
    private int numImmigrants;

    private void setupSurrogate(EvolutionState state, Parameter surBase)
    {
        surLogPath = state.parameters.getString(surBase.push(P_SURR_LOG_PATH), null);
        if(surLogPath == null)
        {
            state.output.fatal("Surrogate log path cannot be null");
            return;
        }
        state.output.warning("Surrogate log path: " + surLogPath);

        surLogID = setupLogger(this, new File(surLogPath, "surr/SurrogatePool.0.csv").getAbsolutePath());

        DMSSaver sstate = (DMSSaver) state;
        KNNSurrogateFitness surrogate = new KNNSurrogateFitness();

        RefRulePhenoTreeSimilarityMetric metric = new RefRulePhenoTreeSimilarityMetric();
        surrogate.setMetric(metric);

        // Number of decision-making situations to consider for phenotypic characterisation.
        int dmsSize = 20;
        surrogate.setSituations(sstate.getInitialSituations().subList(0,
                Math.min(sstate.getInitialSituations().size(), dmsSize)));

        trc = new TaskIndexCharacterisation(sstate.getInitialSituations().subList(0,
                Math.min(sstate.getInitialSituations().size(), dmsSize)));

        int populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        surrogate.setSurrogateUpdatePolicy(
                new FILOPhenotypicUpdatePolicy(2 * populationSize, metric));

        Parameter p = new Parameter("eval.problem.pool-filter");
        PoolFilter filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));
        surrogate.setFilter(filter);

        this.surFitness = surrogate;
    }

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);
        super.setup(this, base);
        setupSurrogate(state, base);

        interimPopLogID = setupLogger(this, new File(surLogPath, "pop/InterimPop.0.csv").getAbsolutePath());
        log(this, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");

        popLogID = setupLogger(this, new File(surLogPath, "pop/Pop.0.csv").getAbsolutePath());
        log(this, popLogID, "SurrogateTime,SurrogateFitness,Fitness,Tree\n");

        Parameter p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

        numImmigrants = state.parameters.getInt(base.push(P_NUM_IMMIGRANTS), null);
        state.output.warning("Number of immigrants: " + numImmigrants + "\n");
    }

    private ArrayList<Population> breedMore()
    {
        int howMany = 2;
        ArrayList<Population> retVal = new ArrayList<>(howMany);
        while (howMany-- > 0)
        {
            Population p = breeder.breedPopulation((this));
            Population ret = (Population) p.emptyClone();
            for(int i = 0; i < p.subpops.length; i++)
                ret.subpops[i].individuals = p.subpops[0].individuals;
            retVal.add(ret);
        }

        return retVal;
    }

    protected RuleCharacterisation<int[]> trc;

    protected PoolFilter filter;

    protected int[] characterise(Individual ind)
    {
        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual) ind).trees[0]);
        return trc.characterise(policy);
    }

    void hashIndividuals(Individual[] individuals,
                         List<Pair<Individual, Integer>> uniques,
                         HashSet<Integer> uniqueIndices,
                         List<Pair<Individual, Integer>> duplicates,
                         HashSet<Integer> duplicateIndices)
    {
        HashSet<Integer> hashedInds = new HashSet<>();

        for (int i = 0; i < individuals.length; i++)
        {
            int[] ci = characterise(individuals[i]);
            int hash = Arrays.hashCode(ci);
            boolean wasNew = hashedInds.add(hash);
            if (wasNew)
            {
                uniques.add(new ImmutablePair<>(individuals[i], i));
                uniqueIndices.add(i);
            } else {
                duplicates.add(new ImmutablePair<>(individuals[i], i));
                duplicateIndices.add(i);
            }
        }
    }

    protected void breed()
    {
        statistics.preBreedingStatistics(this);

        ArrayList<Population> temporaryPop = breedMore();

        List<Pair<Individual, Integer>> uniques = new ArrayList<>();
        HashSet<Integer> uniqueIndices = new HashSet<>();
        HashSet<Integer> duplicateIndices = new HashSet<>();
        List<Pair<Individual, Integer>> duplicates = new ArrayList<>();

        hashIndividuals(temporaryPop.get(0).subpops[0].individuals, uniques, uniqueIndices, duplicates,
                duplicateIndices);

        List<Pair<Individual, Integer>> uniques2 = new ArrayList<>();
        HashSet<Integer> uniqueIndices2 = new HashSet<>();
        HashSet<Integer> duplicateIndices2 = new HashSet<>();
        List<Pair<Individual, Integer>> duplicates2 = new ArrayList<>();

        hashIndividuals(temporaryPop.get(1).subpops[0].individuals, uniques2, uniqueIndices2, duplicates2,
                duplicateIndices2);

        for (int i = 0; i < uniques2.size(); i++)
        {
            Individual ind = uniques2.get(i).getKey();
            double fitness = surFitness.fitness(ind);
            ((MultiObjectiveFitness)ind.fitness).objectives[0] = fitness;
        }

        for (int i = 0; i < numImmigrants; i++)
        {
            int index = PopulationUtils.tournamentSelect(uniques2,
                    Comparator.comparingDouble(o -> o.getKey().fitness.fitness()), this, 0, 7);
            if(index == -1)
                break;

            Pair<Individual, Integer> toInsert = uniques2.remove(index);

            index = PopulationUtils.tournamentSelect(duplicates,
                    Comparator.comparingDouble(o -> -o.getKey().fitness.fitness()), this, 0 , 7);
            Pair<Individual, Integer> toRemove;
            if(index == -1)
            {
                index = PopulationUtils.tournamentSelect(uniques,
                        Comparator.comparingDouble(o -> -o.getKey().fitness.fitness()), this, 0 , 7);
                toRemove = uniques.remove(index);
            }
            else
                toRemove = duplicates.remove(index);

            temporaryPop.get(0).subpops[0].individuals[toRemove.getRight()] = toInsert.getLeft();
        }

        // POST-BREEDING EXCHANGING
        statistics.postBreedingStatistics(this);
    }

    private void updateSurrogate()
    {
        surFitness.updateSurrogatePool(population.subpops[0].individuals, "" + generation);
    }

    @Override
    public int evolve() {

        if (generation > 0)
        {
            output.message("Generation " + generation);
        }

        statistics.preEvaluationStatistics(this);
        evaluator.evaluatePopulation(this);
        statistics.postEvaluationStatistics(this);

        updateSurrogate();

        updateLoggers();

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

    private void updateLoggers()
    {
        closeLogger(this, surLogID);
        surLogID = setupLogger(this, new File(surLogPath, "surr/SurrogatePool." + (generation + 1) + ".csv").getAbsolutePath());

        closeLogger(this, popLogID);
        popLogID = setupLogger(this, new File(surLogPath, "pop/Pop." + (generation + 1) + ".csv").getAbsolutePath());
        log(this, popLogID, "SurrogateTime,SurrogateFitness,Fitness,Tree\n");

        closeLogger(this, interimPopLogID);
        interimPopLogID = setupLogger(this, new File(surLogPath, "pop/InterimPop." + (generation + 1) + ".csv").getAbsolutePath());
        log(this, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");
    }
}
