package tl.knowledge.multipop.multipoptl;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.*;
import ec.util.Parameter;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;
import tl.knowledge.surrogate.SuGPIndividual;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * This is an extended version of the {@code CyclingUniqueFullTreeBuilderEx} class. This class allows using enabling or
 * disabling the duplicate removal procedure. Additionally, as the original class only loaded, and reused, one
 * population size of the transferred individuals, this class allows loading a magnitude of the population size.
 *
 * This class was used in the APTGPSecondBestAux and APTGPNoDupRem experiments.
 */
public class CyclingUniqueFullTreeBuilderEx extends CyclingUniqueFullTreeBuilder
{
    /**
     * Specifies how many times the population size should be loaded. When this class is used in multi-population
     * scenarios, setting this value two 2 (or larger) will initialise the first population with the best individuals
     * and the second population with the second-best individuals. On the other hand, setting the parameter to 1 will
     * make the algorithm initialise all populations with the same best individuals.
     */
    public static final String P_TRANSFER_MAGNITUDE = "transfer-magnitude";
    private int transferMagnitude;

    public static final String P_REMOVE_DUPLICATES = "remove-duplicates";
    private boolean removeDuplicates = true;

    /**
     * A simple counter that will be used as a hash code in the updateHashTable method when duplicate removal is
     * disabled.
     */
    private int dupHashCode = 0;

    /**
     * The index of the good individuals that should be transferred next.
     */
    private int index = 0;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        transferMagnitude = state.parameters.getInt(base.push(P_TRANSFER_MAGNITUDE), null);
        if(transferMagnitude <= 0)
            logFatal(state, knowledgeSuccessLogID, "Invalid transfer magnitude: " + transferMagnitude);
        log(state, knowledgeSuccessLogID, true, "Transfer magnitude: " + transferMagnitude + "\n");

        removeDuplicates = state.parameters.getBoolean(base.push(P_REMOVE_DUPLICATES), null, true);
        log(state, knowledgeSuccessLogID, true, "Remove duplicates: " + removeDuplicates + "\n");
    }

    private void loadSource(EvolutionState state)
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

        goodInds = loadedInds.subList(0, transferMagnitude * populationSize);
    }

    @Override
    protected void updateHashTable(List<? extends Individual> pop, HashMap<Integer, Individual> hashInds)
    {
        for(int j = 0; j < pop.size(); j++)
        {
            Individual ind = pop.get(j);

            if(removeDuplicates) {
                int[] ch = metric.characterise((GPIndividual) ind, 0, filter);
                int hash = Arrays.hashCode(ch);
                if (!hashInds.containsKey(hash)) {
                    if (ind instanceof SuGPIndividual)
                        hashInds.put(hash, ind);
                    else
                        hashInds.put(hash, SuGPIndividual.asGPIndividual((GPIndividual) ind, 0, true));
                }
            }
            else
            {
                if (ind instanceof SuGPIndividual)
                    hashInds.put(dupHashCode++, ind);
                else
                    hashInds.put(dupHashCode++, SuGPIndividual.asGPIndividual((GPIndividual) ind, 0, true));
            }

        }
    }

    @Override
    public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set,
                                int argposition, int requestedSize)
    {
        if(goodInds == null)
        {
            loadSource(state);
        }
        GPNode root;
        if(transferCount <= (int)(transferPercent[activeSubpop] * populationSize))
        {
            GPIndividual ind = (GPIndividual) goodInds.get(index);
            ind = (GPIndividual) ind.clone();
            root = GPIndividualUtils.stripRoots(ind).get(0);
            transferCount++;
            index++;
            if(index >= goodInds.size())
                index = 0;
        }
        else
        {
            root = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
            log(state, knowledgeSuccessLogID, "random," + root.makeLispTree() + "\n");
        }

        root.parent = parent;
        root.argposition = (byte) argposition;
        numIndsCreated++;
        if(numIndsCreated >= populationSize)
        {
            // A bit of warning here. For this to work properly, the 'pop.subpop.*.duplicate-retries' parameter must be
            // set to zero for all sub-populations, or at least for the ones that matter.
            // Also, the sub-populations must have the same size.
            numIndsCreated = 0;
            transferCount = 0;
            activeSubpop++;
        }
        return root;
    }
}
