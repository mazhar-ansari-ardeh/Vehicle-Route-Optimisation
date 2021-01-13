package tl.knowledge;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;
import tl.gp.TLGPIndividual;
import tl.gp.similarity.RefRulePhenoTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.multipop.Mutator;
import tl.knowledge.surrogate.SuGPIndividual;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;
import tl.knowledge.surrogate.knn.UnboundedUpdatePolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class SAMUFullTreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    private int knowledgeSuccessLogID;

    /**
     * The base for parameters of the mutator.
     */
    private static final String P_MUTATOR_BASE = "mutator";
    Mutator mutator = new Mutator();

    /**
     * Normalised fitness threshold for the transferred individuals. The individuals that have a fitness below this
     * value will not be considered for transfer. Normalised fitness values are in the range [0, 1] (higher is better).
     */
    private static final String P_NFITNESS_THRESHOLD = "fit-thresh";
    private double threshold;

    /**
     * The percentage of the initial population to be created from the individuals transferred from the source domain.
     */
    public static final String P_TRANSFER_PERCENT = "transfer-percent";
    private double transferPercent;

    private static final String P_SURR_LOG_PATH = "surr-log-path";
    private String surLogPath;
    private int interimPopLogID;

    /**
     * The magnitude of the interim population, that is, how many times larger it is than the population. A value of
     * zero indicates that no interim population should be created. This parameter cannot have a negative value.
     */
    public final String P_INTERIM_MAGNITUDE = "interim-magnitude";
    private int interimMagnitude;

    public static final String P_INTERIM_FROM_MUTATION = "interim-from-mutation";
    private int interimFromMutation;

    /**
     * Boolean parameter that if set to true, the mutated individuals will be evaluated with a surrogate model.
     * Otherwise, the mutated individuals are given a very large fitness value. In this case, only the mutation operator
     * makes any contributions to the effectiveness of the algorithm.
     */
    public static final String P_ENABLE_SURROGATE = "enable-surr";
    private boolean enableSurrogate = true;

    private ArrayList<Individual> goodInds;

    /**
     * If this parameter is true, then the transferred individuals will be included in the pool of good individuals to
     * be selected if they have a good fitness. Otherwise, only the mutated individuals will be considered for transfer.
     */
    public static final String P_INCLUDE_GOOD_INDS = "incl-good-inds";
    private boolean includeGoodInds = true;

    /**
     * The path to the directory that contains GP populations.
     */
    public static final String P_KNOWLEDGE_PATH = "knowledge-path";
    private String knowledgePath;

    /**
     * The normalised fitness threshold (the lower the better) for selecting the individuals that will create the
     * interim population via mutation. The default value of this parameter is the same as @{code threshold}.
     */
    public static final String P_MUTATION_THRESHOLD = "mutation-threshold";
    private double mutThreshold;

    /**
     * Number of individuals that are transferred, including the mutated ones.
     */
    private int transferCount = 0;

    private int populationSize;
    private KNNSurrogateFitness surFitness;

    private List<Individual> mutatedInds;

    RefRulePhenoTreeSimilarityMetric metric = new RefRulePhenoTreeSimilarityMetric();

    PoolFilter filter;

    /**
     * Maximum fitness value of the transferred individuals.
     */
    private double maxFit;

    /**
     * Minimum fitness value of the transferred individuals.
     */
    private double minFit;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        knowledgeSuccessLogID = setupLogger(state, base, true);

        transferPercent = state.parameters.getDouble(base.push(P_TRANSFER_PERCENT), null);
        if(transferPercent < 0 || transferPercent > 1)
            logFatal(state, knowledgeSuccessLogID, "Invalid transfer percent: " + transferPercent + "\n");
        log(state, knowledgeSuccessLogID, true, "SAMUFullTree: Transfer percent: " + transferPercent + "\n");

        threshold = state.parameters.getDouble(base.push(P_NFITNESS_THRESHOLD), null);
        if(threshold < 0 || threshold > 1)
            logFatal(state, knowledgeSuccessLogID,
                    "Invalid value for the normalised threshold: " + threshold + "\n");
        log(state, knowledgeSuccessLogID, true, "Normalised threshold: " + threshold + "\n");

        mutThreshold = state.parameters.getDoubleWithDefault(base.push(P_MUTATION_THRESHOLD), null, threshold);
        if(mutThreshold < 0 || mutThreshold > 1)
            logFatal(state, knowledgeSuccessLogID,
                    "Invalid value for the mutation threshold: " + mutThreshold + "\n");
        log(state, knowledgeSuccessLogID, true, "Mutation threshold: " + mutThreshold + "\n");

        mutator.setup(state, base.push(P_MUTATOR_BASE));

        populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);

        interimMagnitude = state.parameters.getInt(base.push(P_INTERIM_MAGNITUDE), null);
        if(interimMagnitude < 0)
            logFatal(state, knowledgeSuccessLogID,
                    "Interim magnitude cannot be negative: " + interimMagnitude + "\n");
        log(state, knowledgeSuccessLogID, true, "Interim Magnitude: " + interimMagnitude + "\n");

        interimFromMutation = state.parameters.getInt(base.push(P_INTERIM_FROM_MUTATION), null);
        if(interimFromMutation < 0 || interimFromMutation >= interimMagnitude)
            logFatal(state, knowledgeSuccessLogID,
                    "Invalid interim from mutation value: " + interimFromMutation + "\n");
        log(state, knowledgeSuccessLogID, true, "Interim from mutation: " + interimFromMutation + "\n");

        includeGoodInds = state.parameters.getBoolean(base.push(P_INCLUDE_GOOD_INDS), null, true);
        log(state, knowledgeSuccessLogID, true, "Include good individuals: " + includeGoodInds + "\n");

        enableSurrogate = state.parameters.getBoolean(base.push(P_ENABLE_SURROGATE), null, true);
        log(state, knowledgeSuccessLogID, true, "Enable surrogate: " + enableSurrogate + "\n");

        surLogPath = state.parameters.getString(base.push(P_SURR_LOG_PATH), null);
        if(surLogPath == null)
        {
            state.output.fatal("Surrogate log path cannot be null");
            return;
        }

        knowledgePath = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
        if (knowledgePath == null)
        {
            state.output.fatal("Knowledge path cannot be null");
            return;
        }
        log(state, knowledgeSuccessLogID, true, "Knowledge path: " + knowledgePath + "\n");


        Parameter p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

        state.output.warning("Surrogate log path: " + surLogPath);
        interimPopLogID = setupLogger(state, new File(surLogPath, "pop/InterimPop.0.csv").getAbsolutePath());
        log(state, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");

        DMSSaver saver = (DMSSaver) state;
        List<ReactiveDecisionSituation> initialSituations = saver.getInitialSituations();
        metric.setSituations(initialSituations.subList(0, Math.min(initialSituations.size(), 20)));
    }

    private void setupSurrogate(EvolutionState state, String surLogPath, ArrayList<Individual> loadedInds)
    {
        surFitness = new KNNSurrogateFitness();

        // Almost all other update policy methods do some modifications to the pool. However, in this experiment, I
        // don't want this because I want to have a pool that is as large as possible. I perform all modifications
        // myself and before passing the pool to the KNN and its update policy.
        surFitness.setSurrogateUpdatePolicy(new UnboundedUpdatePolicy());
        surFitness.setMetric(metric);
        DMSSaver saver = (DMSSaver) state;
        List<ReactiveDecisionSituation> initialSituations = saver.getInitialSituations();
        surFitness.setSituations(initialSituations.subList(0, Math.min(initialSituations.size(), 20)));

        surFitness.setFilter(filter);
        surFitness.updateSurrogatePool(loadedInds.toArray(new Individual[0]), "s:gen_0_49");

        int surPoolLogID = setupLogger(state, new File(surLogPath, "surr/SurrogatePool.0.csv").getAbsolutePath());
        log(state, surPoolLogID, surFitness.logSurrogatePool());
        closeLogger(state, surPoolLogID);
    }

    private void updateHashTable(List<? extends Individual> pop, HashMap<Integer, Individual> hashInds)
    {
        for(int j = 0; j < pop.size(); j++)
        {
            Individual ind = pop.get(j);
            int[] ch = metric.characterise((GPIndividual) ind, 0, filter);
            int hash = Arrays.hashCode(ch);
            if(!hashInds.containsKey(hash))
            {
                if(ind instanceof SuGPIndividual)
                    hashInds.put(hash, ind);
                else
                    hashInds.put(hash, SuGPIndividual.asGPIndividual((GPIndividual) ind, 0, true));
            }
        }
    }

    private void updateHashTable(Individual[] pop, HashMap<Integer, Individual> hashInds)
    {
        updateHashTable(Arrays.asList(pop), hashInds);
    }

    private void updateHashTable(Population pop, HashMap<Integer, Individual> hashInds)
    {
        for(int i = 0; i < pop.subpops.length; i++)
            updateHashTable(pop.subpops[i].individuals, hashInds);
    }

    private ArrayList<Individual> loadSource(EvolutionState state)
    {
        HashMap<Integer, Individual> hashInds = new HashMap<>();
        for (int i = 49; i >= 0; i--)
        {
            File file = Paths.get(knowledgePath, "population.gen." + i + ".bin").toFile();
            if(!file.exists())
            {
                log(state, knowledgeSuccessLogID, true, "The file " + file.toString() + " does not exist. Ignoring.\n");
                continue;
            }
            Population pop;
            try {
                pop = PopulationUtils.loadPopulation(file);
            } catch (IOException | ClassNotFoundException e)
            {
                log(state, knowledgeSuccessLogID, true,
                        String.format("Failed to load the file %s. The file is ignored.\nException:\n%s\n",
                                file.toString(), e.toString()));
                e.printStackTrace();
                continue;
            }
            ArrayList<Individual> pool = new ArrayList<>(Arrays.asList(pop.subpops[0].individuals));
            pool.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
            updateHashTable(pop, hashInds);
        }

        ArrayList<Individual> loadedInds = new ArrayList<>(hashInds.values());

        loadedInds.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
        minFit = loadedInds.get(0).fitness.fitness();
        maxFit = loadedInds.get(loadedInds.size() - 1).fitness.fitness();

        goodInds = selectGoodTransferredInds(state, loadedInds, threshold);

        return loadedInds;
    }

    private ArrayList<Individual> selectGoodTransferredInds(EvolutionState state, List<Individual> transferredInds, double nFitThresh)
    {
        if(transferredInds == null || transferredInds.isEmpty())
        {
            logFatal(state, knowledgeSuccessLogID, "The list of transferred individuals is empty.\n");
            return null;
        }

        ArrayList<Individual> goodIndividuals = new ArrayList<>();
        for(Individual rp : transferredInds)
        {
            double fitness = rp.fitness.fitness();
            double normFit = (fitness - minFit) / (maxFit - minFit);

            if(normFit <= nFitThresh)
                goodIndividuals.add(rp);
            else
                break;
        }

        return goodIndividuals;
    }

    private SuGPIndividual mutate(EvolutionState state, TLGPIndividual ind)
    {
        ind = (TLGPIndividual) mutator.mutate(0, ind, state, 0);

        return SuGPIndividual.asGPIndividual(ind, 0, false);
    }

    void createInitPop(EvolutionState state)
    {
        HashMap<Integer, Individual> hashedInterimPop = new HashMap<>(); // Hashed Mutated Individuals
        mutatedInds = new ArrayList<>(populationSize);
        int k = 1;
        if(includeGoodInds)
        {
            int toIncSize = Math.min(populationSize, goodInds.size());
            updateHashTable(goodInds.subList(0, toIncSize), hashedInterimPop);
        }
        List<Individual> toMutate = selectGoodTransferredInds(state, goodInds, mutThreshold);

        while(hashedInterimPop.size() < interimMagnitude * populationSize)
        {
            if(hashedInterimPop.size() > (interimMagnitude - interimFromMutation) * populationSize)
            {
                toMutate = new ArrayList<>(hashedInterimPop.values());
            }
            ArrayList<SuGPIndividual> tempPop = new ArrayList<>();
            for (int i = 0; i < toMutate.size(); i++)
            {
                TLGPIndividual ind = (TLGPIndividual) toMutate.get(i).clone();
                SuGPIndividual suInd = mutate(state, ind);
                log(state, interimPopLogID, "Mutated," + ind.toString() + "," + suInd.toString() + "\n");
                tempPop.add(suInd);
            }

            updateHashTable(tempPop, hashedInterimPop);
            log(state, interimPopLogID, true, ",,Iteration " + k++ + " finished.\n\n");
        }

        mutatedInds = new ArrayList<>(hashedInterimPop.values());
        hashedInterimPop.clear();

        evalSortMutatedInds();

        mutatedInds.forEach(ind -> log(state, interimPopLogID, ((SuGPIndividual)ind).getSurFit() + "," + ind.fitness.fitness()
                + "," + ((GPIndividual)ind).trees[0].child.makeLispTree() + "\n"));
        closeLogger(state, interimPopLogID);

        mutatedInds = mutatedInds.subList(0, ((int) (transferPercent * populationSize))); // + 1 is used just in case so that there is always enough
        goodInds.clear();
        surFitness = null;
    }

    private void evalSortMutatedInds()
    {
        for(int i = 0; i < mutatedInds.size(); i++)
        {
            GPIndividual ind = (GPIndividual) mutatedInds.get(i);
            if(!ind.evaluated)
            {
                SuGPIndividual suInd = (SuGPIndividual) ind;
                ((MultiObjectiveFitness) suInd.fitness).objectives[0] = enableSurrogate ? surFitness.fitness(suInd) : Double.POSITIVE_INFINITY;
                suInd.setSurFit(suInd.fitness.fitness());
            }
        }

        mutatedInds.sort((o1, o2) -> {
            int cmp = Double.compare(o1.fitness.fitness(), o2.fitness.fitness());
            if(cmp != 0)
                return cmp;
            if(o1.evaluated == o2.evaluated)
                return 0;
            if(o1.evaluated)
                return -1;
            return 1; // o2.evaluated == true
        });
    }

    @Override
    public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set,
                                int argposition, int requestedSize)
    {
        if(goodInds == null)
        {
            ArrayList<Individual> loadedInds = loadSource(state);
            setupSurrogate(state, surLogPath, loadedInds);
            createInitPop(state);
        }
        if(transferCount <= (int)(transferPercent * populationSize) && !mutatedInds.isEmpty())
        {
            GPIndividual ind = (GPIndividual) mutatedInds.remove(0);
            GPNode root = GPIndividualUtils.stripRoots(ind).get(0);
            root.parent = parent;
            root.argposition = (byte) argposition;
            transferCount++;
            return root;
        }

        GPNode rand = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
        rand.parent = parent;
        rand.argposition = (byte) argposition;
        log(state, knowledgeSuccessLogID, "random," + rand.makeLispTree() + "\n");
        return rand;
    }
}
