package tl.knowledge.sst;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.TLGPIndividual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;


/**
 * This is an extended version of {@code SSTBuilder} that allows different methods for selecting the transferred
 * individuals. As {@code SSTBuilder} only selected the best individuals, this class allows selecting the worst or
 * selecting randomly.
 */
public class SSTBuilderEx extends HalfBuilder implements TLLogger<GPNode>
{
    private int knowledgeSuccessLogID;

    public static final String P_SORT_TYPE = "sort-type";
    private String sortType;

    /**
     * The percentage of the initial population to be created from the individuals transferred from the source domain.
     */
    public static String P_TRANSFER_PERCENT = "transfer-percent";
    private double transferPercent;

    /**
     * The similarity threshold. If a new individual is within this distance of an already seen individual, then they
     * are considered similar.
     */
    public static String P_SIMILARITY_THRESHOLD = "similarity-thresh";
    private double similarityThreshold;

    private int populationSize;
    private static int cfCounter = 0;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base, true);
        if (!(state instanceof SSTEvolutionState))
        {
            logFatal(state, knowledgeSuccessLogID, "The state is not of type SSTEvolutionState\n");
            return;
        }

        transferPercent = state.parameters.getDouble(base.push(P_TRANSFER_PERCENT), null);
        if(transferPercent < 0 || transferPercent > 1)
            logFatal(state, knowledgeSuccessLogID, "Invalid transfer percent: " + transferPercent + "\n");
        log(state, knowledgeSuccessLogID, true, "SSTBuilder: Transfer percent: " + transferPercent + "\n");

        similarityThreshold = state.parameters.getInt(base.push(P_SIMILARITY_THRESHOLD), null);
        if(similarityThreshold < 0)
            logFatal(state, knowledgeSuccessLogID,"Xover: Invalid similarity threshold: " + similarityThreshold);
        log(state, knowledgeSuccessLogID, true, "SSTBuilder: similarity threshold: " + similarityThreshold + "\n");

        populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);

        sortType = state.parameters.getString(base.push(P_SORT_TYPE), null);
        if(sortType == null)
        {
            state.output.fatal("Sort type cannot be null.");
            return;
        }
        log(state, knowledgeSuccessLogID, true, "Sort type: " + sortType + "\n");
    }

    private ArrayList<GPRoutingPolicy> transferredPop = null;

    @Override
    public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set,
                                int argposition, int requestedSize)
    {
        SSTEvolutionState sstate = (SSTEvolutionState) state;
        if(sstate.createRandInd())
        {
            return super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
        }

        if(transferredPop == null)
        {
            transferredPop = new ArrayList<>(sstate.getTransferredIndividuals());
            switch(sortType.toLowerCase())
            {
                case "ascending": // low to high fitness
                    transferredPop.sort(Comparator.comparingDouble(i -> i.getGPTree().owner.fitness.fitness()));
                    break;
                case "descending": // high to low fitness
                    transferredPop.sort(Comparator.comparing(i -> -i.getGPTree().owner.fitness.fitness()));
                    break;
                case "random":
                    int seed = state.parameters.getInt(new Parameter("seed.0"), null);
                    Random rnd = new Random(seed);
                    Collections.shuffle(transferredPop, rnd);
                    break;
            }
        }

        if(cfCounter < populationSize * transferPercent && !transferredPop.isEmpty())
        {
            GPIndividual ind = transferredPop.remove(0).getGPTree().owner;
            ind = (GPIndividual) ind.clone();

            GPNode root = GPIndividualUtils.stripRoots(ind).get(0);

            cfCounter++;
            log(state, knowledgeSuccessLogID, cfCounter + ": \t" + ind.fitness.fitness() + ", " + root.makeLispTree()
                    + "\ntransferred from source\n\n");
            root.parent = parent;
            root.argposition = (byte) argposition;
            return root;
        }

        TLGPIndividual tlgpIndividual;
        GPNode root = null;
        do
        {
            if(root != null)
                log(state, knowledgeSuccessLogID, "Discarded seen random: " + root.makeLispTree() + "\n\n");
            root = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);

            tlgpIndividual = GPIndividualUtils.asGPIndividual(root);
        }while(!sstate.isNew(tlgpIndividual, similarityThreshold));

        root.parent = parent;
        root.argposition = (byte) argposition;
        tlgpIndividual.trees[0].child = null;

        log(state, knowledgeSuccessLogID, "Unseen random: " + root.makeLispTree() + "\n\n");
        return root;
    }
}