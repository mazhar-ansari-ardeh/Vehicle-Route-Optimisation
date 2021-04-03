package tl.knowledge.multipop.multipoptl;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;
import tl.gp.similarity.RefRulePhenoTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.surrogate.SuGPIndividual;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Implements a transfer of full trees that are phenotypically unique in a cycling mode. That is, this class first
 * loads all the transferred knowledge, then remove duplicates from it and keeps as much as the population size. Then,
 * for transfer, inserts individuals from the retained loaded set. The feature of this class is that if for some reason,
 * such as being used in a multipop scenario, it is needed to transfer more than one population size, the class will
 * start inserting from the begining of the retained set.
 *
 * This class was used in the APTGP project.
 */
public class CyclingUniqueFullTreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    private int knowledgeSuccessLogID;

    /**
     * The percentage of the initial population to be created from the individuals transferred from the source domain.
     */
    public static final String P_TRANSFER_PERCENT = "transfer-percent";
    private double[] transferPercent;


    private List<Individual> goodInds;

    /**
     * The path to the directory that contains GP populations.
     */
    public static final String P_KNOWLEDGE_PATH = "knowledge-path";
    private String knowledgePath;


    /**
     * Number of individuals that are transferred, including the mutated ones.
     */
    private int transferCount = 0;

    /**
     * Number of individuals created for the current subpopulation. The value is reset for each subpopulation.
     */
    private int numIndsCreated = 0;

    private int populationSize;

    private int activeSubpop = 0;

    RefRulePhenoTreeSimilarityMetric metric = new RefRulePhenoTreeSimilarityMetric();

    PoolFilter filter;

    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        knowledgeSuccessLogID = setupLogger(state, base, true);

        populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        int numSubpops = state.parameters.getInt(new Parameter("pop.subpops"), null);

        transferPercent = new double[numSubpops];
        for (int i = 0; i < numSubpops; i++)
        {
            transferPercent[i] = state.parameters.getDouble(base.push(P_TRANSFER_PERCENT).push("" + i), null);
            if(transferPercent[i] < 0 || transferPercent[i] > 1)
                logFatal(state, knowledgeSuccessLogID, "Invalid transfer percent: " + transferPercent[i] + "\n");
            log(state, knowledgeSuccessLogID, true, "SAMUFullTree: Transfer percent: " + transferPercent[i] + "\n");
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

        DMSSaver saver = (DMSSaver) state;
        List<ReactiveDecisionSituation> initialSituations = saver.getInitialSituations();
        metric.setSituations(initialSituations.subList(0, Math.min(initialSituations.size(), 20)));
    }

    private void updateHashTable(List<? extends Individual> pop, HashMap<Integer, Individual> hashInds)
    {
        for(int j = 0; j < pop.size(); j++)
        {
            Individual ind = pop.get(j);
            int[] ch = metric.characterise((GPIndividual) ind, 0, filter);
            int hash = Arrays.hashCode(ch);
            // If duplicate removal is disabled or, enabled and the hash is not seen, insert
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

        goodInds = loadedInds.subList(0, populationSize);
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
            GPIndividual ind = (GPIndividual) goodInds.get(transferCount);
            ind = (GPIndividual) ind.clone();
            root = GPIndividualUtils.stripRoots(ind).get(0);
            transferCount++;
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
