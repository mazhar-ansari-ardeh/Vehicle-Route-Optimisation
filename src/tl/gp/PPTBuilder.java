package tl.gp;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.TLLogger;
import tl.knowledge.ppt.pipe.PPTree;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

/**
 * A builder class that builds a GP individual based on a {@code PPTree} object. The class loads this object with the
 * parameter {@code PPTBuilder.P_KNOWLEDGE_FILE} from a local file. The builder keeps track of the number of objects that it
 * has created and will only use the transferred knowledge if this number is less than the percent of the population that
 * this builder is going to build. <br/>
 *
 * This builder is only adapted to initialize GP and it should not be used for other purposes.
 */
public class PPTBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    /**
     * The path to the file that contains PPT data.
     */
    public static final String P_KNOWLEDGE_FILE = "knowledge-file";

    private int knowledgeSuccessLogID;

    /**
     * The percentage of initial population that is created from extracted knowledge. The value must
     * be in range (0, 1].
     */
    public static final String P_TRANSFER_PERCENT = "transfer-percent";

    /**
     * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
     * population that is created based on the extracted knowledge. The value must be in range (0, 1].
     */
    private double transferPercent;

    private static int cfCounter = 0;
    private PPTree tree;

    @Override
    public void setup(EvolutionState state, Parameter base) {
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base);

        Parameter p = base.push(P_TRANSFER_PERCENT);
        transferPercent = state.parameters.getDouble(p, null);
        if(transferPercent <= 0 || transferPercent > 1)
            state.output.fatal("Transfer percent must be in the range (0, 1]: " + transferPercent);
        else
            state.output.warning("Transfer percent: " + transferPercent);

        String knowFile = state.parameters.getString(base.push(P_KNOWLEDGE_FILE), null);
        if (knowFile == null)
            state.output.fatal("Knowledge file name cannot be null");
        try
        {
            loadPPT(knowFile);
            log(state, knowledgeSuccessLogID, "Loaded the PPT: " + tree.toString());
            if(state instanceof PPTEvolutionState)
            {
                ((PPTEvolutionState)state).setPpt(tree);
                log(state, knowledgeSuccessLogID, "EvolutionState is of type PPTEvolutionState.");
            }
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void loadPPT(String knowFile) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(knowFile));
        tree = (PPTree)oin.readObject();
        oin.close();
    }

    @Override
    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread, final GPNodeParent parent,
                                final GPFunctionSet set, final int argposition, final int requestedSize)
    {
        int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        int numToTransfer = (int) Math.round(popSize * transferPercent);
        if (numToTransfer >= 0 && cfCounter < numToTransfer)
        {
            GPNode cf = tree.sampleIndividual(state.random[thread]);

            if (cf != null)
            {
                cfCounter++;
                log(state, knowledgeSuccessLogID, cfCounter + ": \t" + cf.makeCTree(true, true, true) + "\n\n");
                cf.parent = parent;
                cf.argposition = (byte) argposition;
                return cf;
            }
            else
                log(state, null, cfCounter, knowledgeSuccessLogID);
        }
        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth, type, thread, parent,
                            argposition, set);
        else
            return fullNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth, type, thread, parent,
                            argposition, set);
    }
}
