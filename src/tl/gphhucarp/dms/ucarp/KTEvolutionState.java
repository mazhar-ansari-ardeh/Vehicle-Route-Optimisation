package tl.gphhucarp.dms.ucarp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.PopulationUtils;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSavingGPHHState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Knowledge transferring Evolution State implements a base class that extends the DMSSavingGPHHState class and adds the
 * ability of loading a knowledge source, removing duplicates from it and holding the loaded knowledge for any other
 * GP component to use.
 */
public class KTEvolutionState extends DMSSavingGPHHState
{
    public static final String P_BASE = "kt-state";

    public static final String P_FROM_GENERATION = "from-gen";
    public static final String P_TO_GENERATION = "to-gen";

    /**
     * The distance metric that the clearing procedure uses. Acceptable values are (case insensitive):
     *  - phenotypic
     *  - corrphenotypic
     *  - hamphenotypic
     */
    public static final String P_DISTANCE_METRIC = "distance-metric";
    SituationBasedTreeSimilarityMetric metrics;

    /**
     * The path to the file or directory that contains GP populations.
     */
    public static final String P_KNOWLEDGE_PATH = "knowledge-path";

    /**
     * The radius of the clearing method used for loading the source domain. A negative radius value will disable the
     * clearing operation.
     */
    public static final String P_TRANSFER_RADIUS = "transfer-clear-radius";

    /**
     * The capacity of the clearing method used for loading the source domain.
     */
    public static final String P_TRANSFER_CAPACITY = "transfer-clear-capacity";

    /**
     * Size of the decision-making situations.
     */
    public final String P_DMS_SIZE = "dms-size";
    protected int dmsSize;

    /**
     * The individuals that are transferred from the source domain. This list is loaded once and is not updated
     * afterwards.
     */
    protected List<GPRoutingPolicy> transferredInds = new ArrayList<>();

    PoolFilter filter;
    private int knowledgeSuccessLogID;

    /**
     * Gets the individuals that were loaded/transferred from the source domain. This function returns the exact
     * individuals that were loaded and does not create a deep or shallow copy of then. As a result, any changes applied
     * to the return value will affect the actual values.
     * @return Individuals loaded from the source domain. In case the transfer is disabled, an empty list will be
     * returned.
     */
    public List<GPRoutingPolicy> getTransferredIndividuals()
    {
        return transferredInds;
    }

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base, true);

        Parameter p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

        String metricParam = state.parameters.getString(base.push(P_DISTANCE_METRIC), null);
        log(state, knowledgeSuccessLogID, true, "Similarity metric: " + metricParam + "\n");

        if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
            metrics = new CorrPhenoTreeSimilarityMetric();
        else if(metricParam.equalsIgnoreCase("Phenotypic"))
            metrics = new PhenotypicTreeSimilarityMetric();
        else if(metricParam.equalsIgnoreCase("Hamming"))
            metrics = new HammingPhenoTreeSimilarityMetric();
        else
        {
            state.output.fatal("Unknown distance metric");
            return;
        }

        dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
        if(dmsSize <=0 )
            logFatal(state, knowledgeSuccessLogID, "DMS size cannot be a negative value: " + dmsSize + "\n");
        else
            log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

        metrics.setSituations(getInitialSituations().subList(0,	Math.min(getInitialSituations().size(), dmsSize)));

        loadTransferredInds(state, base);
    }

    private void loadTransferredInds(EvolutionState state, Parameter base)
    {
        double clearRadius = state.parameters.getDouble(base.push(P_TRANSFER_RADIUS), null);
        log(state, knowledgeSuccessLogID, true, "Clearing radius of transferred knowledge: " + clearRadius + "\n");

        int clearCapacity = state.parameters.getInt(base.push(P_TRANSFER_CAPACITY), null);
        if(clearCapacity < 0)
            logFatal(state, knowledgeSuccessLogID,
                    "Clear capacity cannot be zero or negative: " + clearCapacity + "\n");
        else
            log(state, knowledgeSuccessLogID, true,
                "Clearing capacity of transferred knowledge:" + clearCapacity + "\n");

        int fromGen = state.parameters.getInt(base.push(P_FROM_GENERATION), null);
        if(fromGen < 0)
            logFatal(state, knowledgeSuccessLogID, "Cannot load from a negative generation: " + fromGen + "\n");
        log(state, knowledgeSuccessLogID, true, "Load from generation: " + fromGen + "\n");

        int toGen = state.parameters.getInt(base.push(P_TO_GENERATION), null);
        if(toGen < 0)
            logFatal(state, knowledgeSuccessLogID, "Cannot load up to a negative generation: " + toGen + "\n");
        log(state, knowledgeSuccessLogID, true, "Load from generation: " + toGen + "\n");

        String knowledgePath = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
        if (knowledgePath == null)
        {
            state.output.fatal("Knowledge path cannot be null");
            return;
        }
        log(state, knowledgeSuccessLogID, true, "Knowledge path: " + knowledgePath + "\n");

        List<Individual> inds;
        try
        {
            if(clearRadius < 0)
                inds = PopulationUtils.loadPopulations(state, knowledgePath, fromGen, toGen, this, knowledgeSuccessLogID, true);
            else
                inds = PopulationUtils.loadPopulations(state, knowledgePath, fromGen, toGen, filter, metrics,
                    clearRadius, clearCapacity, this, knowledgeSuccessLogID, true);
            if(inds == null || inds.isEmpty())
                throw new RuntimeException("Could not load the saved populations");
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
            state.output.fatal("Failed to load the population from: " + knowledgePath);
            return;
        }

        transferredInds.addAll(
                inds.stream().map(
                        i -> new GPRoutingPolicy(filter, ((GPIndividual)i).trees[0])).collect(Collectors.toList()));
    }

    public void clearTransferredKnowledge()
    {
        if(transferredInds != null)
        {
            transferredInds = null;
        }
    }
}
