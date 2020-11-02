package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.GPHHEvolutionState;
import tl.knowledge.sst.lsh.LSH;

import java.util.*;

public class MultiPopEvolutionState extends GPHHEvolutionState implements MultiPopDMSSaver
{
    protected List<TreeMap<Individual, List<DecisionSituation>>> seenSituations = new ArrayList<>();

    private final List<ReactiveDecisionSituation> initialSituations = new ArrayList<>();

    /**
     * If {@code true}, the seen decision situations will be saved.
     */
    public final String P_SAVE_DMS = "save-dms";
    private boolean saveDMS = true;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        Parameter p = new Parameter(P_SAVE_DMS);
        saveDMS = parameters.getBoolean(p, null,true);
        super.setup(state, base);

        int numSubpop = this.parameters.getInt(new Parameter("pop.subpops"), null);
        for(int i = 0; i < numSubpop; i++)
        {
            seenSituations.add(new TreeMap<>());
        }
    }

    @Override
    public void updateSeenSituations(Individual ind, List<DecisionSituation> situations)
    {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public List<ReactiveDecisionSituation> getAllSeenSituations()
    {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void updateSeenSituations(int subPop, Individual ind, List<DecisionSituation> situations)
    {
        if(!this.saveDMS)
            return;

        if(seenSituations.size() > 1)
            return;

        List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
        situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
        TreeMap<Individual, List<DecisionSituation>> theMap = seenSituations.get(subPop);
        theMap.put(ind, clonedSituations);
        if(theMap.size() > 5)
        {
            theMap.remove(theMap.lastKey());
        }
    }

    @Override
    public List<ReactiveDecisionSituation> getAllSeenSituations(int subPop)
    {
        ArrayList<ReactiveDecisionSituation> retval = new ArrayList<>();
        Set<Map.Entry<Individual, List<DecisionSituation>>> a = seenSituations.get(subPop).entrySet();
        for(Map.Entry<Individual, List<DecisionSituation>> e : a)
        {
            List<DecisionSituation> situations = e.getValue();
            for(DecisionSituation situation : situations)
            {
                ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
                if(rds.getPool().size() >= 2) // greater than two because the correlation-based phenotypics require it.
                    retval.add(rds);
            }
        }

        for(Individual ind : seenSituations.get(subPop).keySet())
        {
            List<DecisionSituation> situations = seenSituations.get(subPop).get(ind);
            for(DecisionSituation situation : situations)
            {
                ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
                if(rds.getPool().size() > 0)
                    retval.add(rds);
            }
        }

        return retval;
    }

    @Override
    public List<ReactiveDecisionSituation> getInitialSituations()
    {
        return initialSituations;
    }

    @Override
    public void addToInitialSituations(DecisionSituation initialSituation)
    {
        if(this.initialSituations.size()> 10000)
            return;
        this.initialSituations.add((ReactiveDecisionSituation) initialSituation);
    }

    @Override
    public void setDMSSavingEnabled(boolean enabled)
    {
        this.saveDMS = enabled;
    }

    @Override
    public boolean isDMSSavingEnabled()
    {
        return this.saveDMS;
    }

    @Override
    protected void rotateEvalModel()
    {
        if (rotateEvalModel) {
            MultiPopReactiveGPHHProblem problem = (MultiPopReactiveGPHHProblem)evaluator.p_problem;
            problem.rotateEvaluationModel();
        }
    }
}