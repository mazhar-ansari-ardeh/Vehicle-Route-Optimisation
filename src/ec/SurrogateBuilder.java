package ec;

import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.gp.SurrogatedGPHHEState;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SurrogateBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    /**
     * The path to the file that contains PPT data.
     */
    public static final String P_KNOWLEDGE_FILE = "knowledge-file";

    /**
     * The percentage of initial population that is created from extracted knowledge. The value must
     * be in range [0, 1].
     */
    private static final String P_TRANSFER_PERCENT = "transfer-percent";
    private double transferPercent;

    /**
     * A boolean parameter to indicate if the builder should initialise the surrogate pool of the state object or not.
     * If this parameter is {@code false} (the default), the builder will only transfer the individuals from the source
     * domain but will not transfer a surrogate. The state object should decide what to do when the surrogate pool is empty.
     */
    private static final String P_INIT_SURROGATE_POOL = "init-surrogate-pool";

//    public static final String P_ALLOW_DUPLICATES = "allow-duplicates";

    private int knowledgeSuccessLogID;

    private static int cfCounter = 0;
    private ArrayList<Individual> pop = new ArrayList<>();
    private int populationSize;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base);

        Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
        transferPercent = state.parameters.getDouble(transferPercentParam, null);
        if(transferPercent < 0 || transferPercent > 1)
        {
            log(state, knowledgeSuccessLogID, "Transfer percent must be in [0, 1]");
            state.output.fatal("Transfer percent must be in [0, 1]");
        }
        else
        {
            log(state, knowledgeSuccessLogID, "Transfer percent: " + transferPercent);
        }

        boolean initSurrogatePool = state.parameters.getBoolean(base.push(P_INIT_SURROGATE_POOL), null, false);
        state.output.warning("Init surrogate pool: " + initSurrogatePool);

        String fileName = state.parameters.getString(base.push(P_KNOWLEDGE_FILE), null);
        if (fileName == null)
        {
            state.output.fatal("Knowledge file name cannot be null");
            return;
        }
        File kbFile = new File(fileName);
        if(!kbFile.exists())
        {
            state.output.fatal("Knowledge file does not exist: " + fileName);
        }

        SurrogatedGPHHEState sstate = (SurrogatedGPHHEState) state;
        try
        {
            Individual[] inds = PopulationUtils.loadPopulation(kbFile).subpops[0].individuals;
            sstate.surFitness.setSituations(sstate.initialSituations.subList(0,
                    sstate.initialSituations.size() > sstate.DMS_SIZE ? sstate.DMS_SIZE : sstate.initialSituations.size()));
            if(initSurrogatePool)
            {
                // TODO: Get pool filter here.
                sstate.surFitness.updateSurrogatePool(inds, "s:gen_49");
            }
            pop.addAll(sstate.surFitness.getSurrogatePool());
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
            state.output.fatal("Failed to load the population file: " + kbFile.getAbsolutePath());
        }

        populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
    }

    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
                                final GPNodeParent parent, final GPFunctionSet set, final int argposition,
                                final int requestedSize)
    {

        if(!pop.isEmpty() && cfCounter < populationSize * transferPercent)
        {
            GPIndividual cf = (GPIndividual) pop.remove(0);cf.trees[0].child.clone();
            GPNode root = (GPNode) cf.trees[0].child.clone();
            cfCounter++;
            log(state, knowledgeSuccessLogID, cfCounter + ": \t" + root.makeCTree(false, true, true) + "\n\n");
            root.parent = parent;
            return root;
        }

        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
        else
            return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
    }
}
