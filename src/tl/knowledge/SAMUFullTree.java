package tl.knowledge;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.TLGPIndividual;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gphhucarp.dms.ucarp.KTEvolutionState;
import tl.knowledge.multipop.Mutator;
import tl.knowledge.surrogate.SuGPIndividual;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;
import tl.knowledge.surrogate.knn.UnboundedUpdatePolicy;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SAMUFullTree extends HalfBuilder implements TLLogger<GPNode>
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

    /**
     * If true, the class will clear the list of transferred individuals in the state object.
     */
    private static final String P_CLEAR_STATE = "clear-state";
    private boolean clear;

    private static final String P_SURR_LOG_PATH = "surr-log-path";
    private String surLogPath;
    private int interimPopLogID;

    private ArrayList<Individual> goodInds;

    /**
     * If this parameter is true, then the transferred individuals will be included in the pool of good individuals to
     * be selected if they have a good fitness. Otherwise, only the mutated individuals will be considered for transfer.
     */
    public static final String P_INCLUDE_GOOD_INDS = "incl-good-inds";
    private boolean includeGoodInds = true;

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

    /**
     * Number of individuals that are transferred, including the mutated ones.
     */
    private int transferCount = 0;

    private int populationSize;
    private KNNSurrogateFitness surFitness;

    private List<Individual> mutatedInds;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        knowledgeSuccessLogID = setupLogger(state, base, true);

        if (!(state instanceof KTEvolutionState))
        {
            logFatal(state, knowledgeSuccessLogID, "Evolution state must be of type KTEvolutionState");
            return;
        }
        KTEvolutionState ktstate = (KTEvolutionState) state;

        transferPercent = state.parameters.getDouble(base.push(P_TRANSFER_PERCENT), null);
        if(transferPercent < 0 || transferPercent > 1)
            logFatal(state, knowledgeSuccessLogID, "Invalid transfer percent: " + transferPercent + "\n");
        log(state, knowledgeSuccessLogID, true, "SSTBuilder: Transfer percent: " + transferPercent + "\n");

        threshold = state.parameters.getDouble(base.push(P_NFITNESS_THRESHOLD), null);
        if(threshold <= 0 || threshold > 1)
            logFatal(state, knowledgeSuccessLogID,
                    "Invalid value for the normalised threshold: " + threshold + "\n");

        mutator.setup(state, base.push(P_MUTATOR_BASE));

        populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);

        interimMagnitude = state.parameters.getInt(base.push(P_INTERIM_MAGNITUDE), null);
        if(interimMagnitude < 0)
            logFatal(state, knowledgeSuccessLogID,
                    "Interim magnitude cannot be negative: " + interimMagnitude + "\n");
        log(state, knowledgeSuccessLogID, "Interim Magnitude: " + interimMagnitude + "\n");

        interimFromMutation = state.parameters.getInt(base.push(P_INTERIM_FROM_MUTATION), null);
        if(interimFromMutation < 0 || interimFromMutation >= interimMagnitude)
            logFatal(state, knowledgeSuccessLogID,
                    "Invalid interim from mutation value: " + interimFromMutation + "\n");
        log(state, knowledgeSuccessLogID, "Interim from mutation: " + interimFromMutation + "\n");

        clear = ktstate.parameters.getBoolean(base.push(P_CLEAR_STATE), null, true);

        includeGoodInds = ktstate.parameters.getBoolean(base.push(P_INCLUDE_GOOD_INDS), null, true);
        log(ktstate, knowledgeSuccessLogID, "Include good individuals: " + includeGoodInds + "\n");

        enableSurrogate = ktstate.parameters.getBoolean(base.push(P_ENABLE_SURROGATE), null, true);
        log(ktstate, knowledgeSuccessLogID, true, "Enable surrogate: " + enableSurrogate + "\n");

        surLogPath = state.parameters.getString(base.push(P_SURR_LOG_PATH), null);
        if(surLogPath == null)
        {
            state.output.fatal("Surrogate log path cannot be null");
            return;
        }
        state.output.warning("Surrogate log path: " + surLogPath);
        interimPopLogID = setupLogger(state, new File(surLogPath, "pop/InterimPop.0.csv").getAbsolutePath());
        log(state, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");
    }

    private void setupSurrogate(EvolutionState state, String surLogPath)
    {
        KTEvolutionState ktstate = (KTEvolutionState) state; // This is checked in the 'setup' method.
        surFitness = new KNNSurrogateFitness();

        // Almost all other update policy methods do some modifications to the pool. However, in this experiment, I
        // don't want this because I want to have a pool that is as large as possible. I perform all modifications
        // myself and before passing the pool to the KNN and its update policy.
        surFitness.setSurrogateUpdatePolicy(new UnboundedUpdatePolicy());
        surFitness.setMetric(ktstate.getSimilarityMetric());
        surFitness.setSituations(ktstate.getInitialSituations().subList(0,
                Math.min(ktstate.getInitialSituations().size(), ktstate.getDMSSize())));

        surFitness.setFilter(ktstate.getFilter());
        surFitness.updateSurrogatePool(
                ktstate.getTransferredIndividuals().stream().map(rp -> rp.getGPTree().owner).toArray(Individual[]::new),
                "s:gen_0_49");

        int surPoolLogID = setupLogger(state, new File(surLogPath, "surr/SurrogatePool.0.csv").getAbsolutePath());
        log(state, surPoolLogID, surFitness.logSurrogatePool());
        closeLogger(state, surPoolLogID);
    }

    private void makeTheTransfer(KTEvolutionState ktstate)
    {
        setupSurrogate(ktstate, surLogPath);
        List<GPRoutingPolicy> transferredInds = ktstate.getTransferredIndividuals();
        if(transferredInds == null || transferredInds.isEmpty())
        {
            logFatal(ktstate, knowledgeSuccessLogID, "The list of transferred individuals is empty.\n");
            return;
        }
        transferredInds.sort(Comparator.comparingDouble(i -> i.getGPTree().owner.fitness.fitness()));

        double minFit = transferredInds.get(0).getGPTree().owner.fitness.fitness();
        double maxFit = transferredInds.get(transferredInds.size() - 1).getGPTree().owner.fitness.fitness();

        goodInds = new ArrayList<>();
        for(GPRoutingPolicy rp : transferredInds)
        {
            double fitness = rp.getGPTree().owner.fitness.fitness();
            double normFit = (maxFit - fitness) / (maxFit - minFit);

            if(normFit >= threshold)
                goodInds.add(rp.getGPTree().owner);
            else
                break;
        }

        if(clear)
            ktstate.clearTransferredKnowledge();
    }

    void createInitPop(final EvolutionState state)
    {
        mutatedInds = new ArrayList<>(populationSize);
        KTEvolutionState ktstate = (KTEvolutionState) state;
        int k = 1;
        List<Individual> toMutate = goodInds;
        if(includeGoodInds)
            mutatedInds.addAll(goodInds.stream().map(i -> SuGPIndividual.asGPIndividual((GPIndividual) i, 0, true)).collect(Collectors.toList()));
        while(mutatedInds.size() < interimMagnitude * populationSize)
        {
            if(mutatedInds.size() > (interimMagnitude - interimFromMutation) * populationSize)
            {
                toMutate = mutatedInds;
            }
            ArrayList<SuGPIndividual> tempPop = new ArrayList<>();
            for (int i = 0; i < toMutate.size(); i++)
            {
                TLGPIndividual ind = (TLGPIndividual) toMutate.get(0).clone();
                ind = (TLGPIndividual) mutator.mutate(0, ind, ktstate, 0);

                SuGPIndividual suInd = SuGPIndividual.asGPIndividual(ind, 0, false);
                if(enableSurrogate)
                    ((MultiObjectiveFitness) suInd.fitness).objectives[0] = surFitness.fitness(ind);
                else
                    ((MultiObjectiveFitness) suInd.fitness).objectives[0] = 2 * goodInds.get(goodInds.size() - 1).fitness.fitness();
                suInd.setSurFit(ind.fitness.fitness());

                tempPop.add(suInd);
            }
            tempPop.forEach(ind -> log(state, interimPopLogID, ind.getSurFit() + "," + ind.fitness.fitness()
                    + "," + ind.trees[0].child.makeLispTree() + "\n"));
            mutatedInds.addAll(tempPop);
            mutatedInds.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
            SimpleNichingAlgorithm.clearPopulation(mutatedInds, ktstate.getFilter(), ktstate.getSimilarityMetric(), 0, 1);
            log(state, interimPopLogID, ",,Iteration " + k++ + " finished.\n\n");

            mutatedInds = mutatedInds.stream().filter(
                    i -> (i.fitness.fitness() != Double.POSITIVE_INFINITY) && i.fitness.fitness() != Double.NEGATIVE_INFINITY)
                    .collect(Collectors.toList());
        }
        mutatedInds.forEach(ind -> log(state, interimPopLogID, ((SuGPIndividual)ind).getSurFit() + "," + ind.fitness.fitness()
                + "," + ((GPIndividual)ind).trees[0].child.makeLispTree() + "\n"));
        closeLogger(state, interimPopLogID);

        mutatedInds.sort((o1, o2) -> {
            int cmp = Double.compare(o1.fitness.fitness(), o2.fitness.fitness());
            if(cmp != 0)
                return cmp;
            if(((SuGPIndividual)o1).evaluated == ((SuGPIndividual)o2).evaluated)
                return 0;
            if(((SuGPIndividual)o1).evaluated)
                return -1;
            if(((SuGPIndividual)o2).evaluated)
                return 1;
            return 0;
        });
        mutatedInds = mutatedInds.subList(0, ((int) (transferPercent * populationSize)) + 1); // + 1 is used just in case so that there is always enough
        goodInds.clear();
    }

    @Override
    public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set,
                                int argposition, int requestedSize)
    {
        if(goodInds == null)
        {
            makeTheTransfer((KTEvolutionState) state);
            createInitPop(state);
        }
        if(transferCount <= (int)(transferPercent * populationSize))
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
