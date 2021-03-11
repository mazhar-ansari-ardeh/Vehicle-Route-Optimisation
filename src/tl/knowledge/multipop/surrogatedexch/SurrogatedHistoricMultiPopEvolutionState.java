package tl.knowledge.multipop.surrogatedexch;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import tl.gp.characterisation.RefRuleCharacterisation;
import tl.knowledge.multipop.HistoricMultiPopEvolutionState;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.surrogate.SuGPIndividual;
import tl.knowledge.surrogate.lsh.LSHSurrogate;

import java.util.List;

public class SurrogatedHistoricMultiPopEvolutionState extends HistoricMultiPopEvolutionState
{
    /**
     * The neighborhoud size to consider.
     */
    public static final String P_K = "k";
    private int k;

    public static final String P_BASE = "shmt-state";

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
    }

//    public void surrogateEvaluate(int supPopToEval, int surrogateToUse)
//    {
//        LSH lsh = history.get(surrogateToUse);
//        LSHSurrogate surrogate = new LSHSurrogate(lsh, filter, trc, k);
//
//        Individual[] subpop = this.population.subpops[supPopToEval].individuals;
//        for(int i = 0; i < subpop.length; i++)
//        {
//            double surFitness = surrogate.fitness(subpop[i]);
//            ((SuGPIndividual)subpop[i]).setSurFit(surFitness);
//        }
//    }

    public void surrogateEvaluate(Individual[] inds, int surrogateToUse)
    {
        LSH lsh = history.get(surrogateToUse);
        LSHSurrogate surrogate = new LSHSurrogate(lsh, filter, trc, k);

        for(int i = 0; i < inds.length; i++)
        {
            double surFitness = surrogate.fitness(inds[i]);
            ((SuGPIndividual) inds[i]).setSurFit(surFitness);
        }
    }
}
