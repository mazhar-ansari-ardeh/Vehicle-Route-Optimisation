package tl.gp;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.TLLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * @author mazhar
 */
public class SimplifiedTreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    private static final long serialVersionUID = 1L;

    /**
     * The percentage of initial population that is created from extracted knowledge. The value must
     * be in range (0, 1].
     */
    public static final String P_TRANSFER_PERCENT = "transfer-percent";


    /**
     * The path to the file that contains subtree information.
     */
    public static final String P_KNOWLEDGE_FILE = "knowledge-file";


    private int knowledgeSuccessLogID;

    /**
     * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
     * population that is transfered from extracted knowledge. The value must be in range (0, 1].
     */
    private double transferPercent;

    private static int cfCounter = 0;
    private Iterator<GPIndividual> iter;

    @Override
    public void setup(EvolutionState state, Parameter base) {
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base);

        Parameter p = base.push(P_TRANSFER_PERCENT);
        transferPercent = state.parameters.getDouble(p, null);

        String knowFile = state.parameters.getString(base.push(P_KNOWLEDGE_FILE), null);
        if (knowFile == null)
            state.output.fatal("Knowledge file name cannot be null");
        loadSubtrees(state, knowFile);
    }


    @SuppressWarnings("unchecked")
    private void loadSubtrees(EvolutionState state, String knowFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(knowFile))) {
            double minFit = ois.readDouble(); // read minFit
            double maxFit = ois.readDouble(); // read maxFit
            ArrayList<GPIndividual> trees = (ArrayList<GPIndividual>) ois.readObject();

            log(state, knowledgeSuccessLogID, "Loaded knowledge base. MinFit: " + minFit + ", maxFit: " + maxFit + ". "
                    + "Database size: " + trees.size() + "\n");

            trees.sort(Comparator.comparingDouble(o -> o.fitness.fitness()));
            // sorted knowledge base
            iter = trees.iterator();

        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
                                final GPNodeParent parent, final GPFunctionSet set, final int argposition,
                                final int requestedSize)
    {
        int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        int numToTransfer = (int) Math.round(popSize * transferPercent);
        if (numToTransfer >= 0 && cfCounter < numToTransfer && iter.hasNext()) {
            GPIndividual cf = iter.next();

            if (cf != null) {
                cfCounter++;
                double fitness = cf.fitness.fitness();
                GPNode node = GPIndividualUtils.stripRoots((GPIndividual) cf.clone()).get(0);
                log(state, knowledgeSuccessLogID, node.makeCTree(true, true, true)+ " contrib: " + fitness + "\n\n");
                node.parent = parent;
                node.argposition = (byte) argposition;
                return node;
            } else
                log(state, null, cfCounter, knowledgeSuccessLogID);
        }
        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth,
                    type, thread, parent, argposition, set);
        else
            return fullNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth,
                    type, thread, parent, argposition, set);
    }

}
