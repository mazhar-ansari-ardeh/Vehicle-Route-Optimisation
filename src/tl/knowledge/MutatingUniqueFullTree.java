package tl.knowledge;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gphhucarp.dms.ucarp.KTEvolutionState;
import tl.knowledge.multipop.Mutator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MutatingUniqueFullTree extends HalfBuilder implements TLLogger<GPNode>
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

    public static final String P_NUM_MUTATIONS = "num-mutations";
    int numMutations;

    /**
     * If true, the class will clear the list of transferred individuals in the state object.
     */
    private static final String P_CLEAR_STATE = "clear-state";
    private boolean clear;

    private ArrayList<GPIndividual> goodInds;

    /**
     * Number of individuals that are transferred, including the mutated ones.
     */
    private int transferCount = 0;

    private int populationSize;

    int timesMutated = 0;
    int goodIndex = 0;

    @Override
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

        numMutations = state.parameters.getInt(base.push(P_NUM_MUTATIONS), null);
        if(numMutations < 0)
            logFatal(state, knowledgeSuccessLogID,
                    "Number of mutations cannot be negative: " + numMutations + "\n");
        log(state, knowledgeSuccessLogID, true, "Number of mutations: " + numMutations + "\n");

        clear = ktstate.parameters.getBoolean(base.push(P_CLEAR_STATE), null, true);
    }

    private void makeTheTransfer(KTEvolutionState ktstate)
    {
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

    @Override
    public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set, int argposition, int requestedSize)
    {
        if(goodInds == null)
            makeTheTransfer((KTEvolutionState) state);

        if(transferCount <= (int)(transferPercent * populationSize))
        {
            if(transferCount < goodInds.size())
            {
                GPIndividual ind = (GPIndividual) goodInds.get(transferCount).clone();
                transferCount++;
                log(state, knowledgeSuccessLogID, "transferred," + ind.trees[0].child.makeLispTree() + "\n");

                GPNode root = GPIndividualUtils.stripRoots(ind).get(0);
                root.parent = parent;
                root.argposition = (byte) argposition;
                return root;
            }

            if(timesMutated >= numMutations)
            {
                goodIndex = (goodIndex + 1) % goodInds.size();
                timesMutated = 0;
            }

            GPIndividual ind = (GPIndividual) goodInds.get(goodIndex).clone();
            log(state, knowledgeSuccessLogID, "mutated," + ind.trees[0].child.makeLispTree() + ",");
            ind = (GPIndividual) mutator.mutate(0, ind, state, 0);
            transferCount++;
            timesMutated++;
            log(state, knowledgeSuccessLogID, ind.trees[0].child.makeLispTree() + "\n");
            GPNode root = GPIndividualUtils.stripRoots(ind).get(0);
            root.parent = parent;
            root.argposition = (byte) argposition;
            return root;
        }

        GPNode rand = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
        rand.parent = parent;
        rand.argposition = (byte) argposition;
        log(state, knowledgeSuccessLogID, "random," + rand.makeLispTree() + "\n");
        return rand;
    }
}
