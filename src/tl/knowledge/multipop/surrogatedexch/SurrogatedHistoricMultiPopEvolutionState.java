package tl.knowledge.multipop.surrogatedexch;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import tl.gp.characterisation.RefRuleCharacterisation;
import tl.gp.similarity.RefRulePhenoTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;
import tl.knowledge.multipop.HistoricMultiPopEvolutionState;
import tl.knowledge.surrogate.SuGPIndividual;
import tl.knowledge.surrogate.knn.FILOPhenotypicUpdatePolicy;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;

import java.util.List;

public class SurrogatedHistoricMultiPopEvolutionState extends HistoricMultiPopEvolutionState
{
    /**
     * The neighborhoud size to consider.
     */
    public static final String P_K = "k";
    private int k;

    public static final String P_BASE = "shmt-state";
    private int lastUpdateGen = -1;

    private KNNSurrogateFitness[] surrogates;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_BASE);

        super.setup(state, base);
        List<ReactiveDecisionSituation> initialSituations = getInitialSituations();
        initialSituations = initialSituations.subList(0, Math.min(dmsSize, initialSituations.size()));
        trc = new RefRuleCharacterisation(initialSituations, new PathScanning5Policy());

        String kStr = parameters.getString(base.push(P_K), null);
        if(kStr.toLowerCase().equals("inf"))
            k = Integer.MAX_VALUE;
        else
            k = parameters.getInt(base.push(P_K), null);
        if(k <= 0)
            output.fatal("K must be a positive integer: " + k + "\n");

        int nSubpop = parameters.getInt(new Parameter("pop.subpops"), null);
        surrogates = new KNNSurrogateFitness[nSubpop];
        for(int i = 0; i < surrogates.length; i++)
            surrogates[i] = setupKNNSurrogates(state, base);
    }

    private KNNSurrogateFitness setupKNNSurrogates(EvolutionState state, Parameter base)
    {
        DMSSaver sstate = (DMSSaver) state;
        KNNSurrogateFitness surrogate = new KNNSurrogateFitness();

        RefRulePhenoTreeSimilarityMetric metric = new RefRulePhenoTreeSimilarityMetric();
        surrogate.setMetric(metric);
        surrogate.setSituations(sstate.getInitialSituations().subList(0,
                Math.min(sstate.getInitialSituations().size(), dmsSize)));

        int populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        surrogate.setSurrogateUpdatePolicy(
                new FILOPhenotypicUpdatePolicy(2 * populationSize, metric));

        Parameter p = new Parameter("eval.problem.pool-filter");
        PoolFilter filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));
        surrogate.setFilter(filter);

        return surrogate;
    }

    public void surrogateEvaluate(Individual[] inds, int surrogateToUse)
    {
        if(generation != lastUpdateGen)
        {
            lastUpdateGen = generation;
            // Sub-population zero is always the main sub-population and it is always evaluated first.
            // Cloning is important because some KNN pool update algorithms may modify the individuals.

            for(int i = 0; i < surrogates.length; i++)
            {
                Individual[] individuals = population.subpops[i].individuals.clone();
                surrogates[i].updateSurrogatePool(individuals, "");
            }
        }

        for(int i = 0; i < inds.length; i++)
        {
            double surFitness = surrogates[surrogateToUse].fitness(inds[i]);
            ((SuGPIndividual) inds[i]).setSurFit(surFitness);
        }
    }
}
