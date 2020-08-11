package gphhucarp.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.evaluation.EvaluationModel;
import tl.gphhucarp.dms.DMSSaver;

import java.util.ArrayList;
import java.util.List;

/**
 * A reactive GPHH problem to evaluate a reactive routing policy during the GPHH.
 * The evaluationg model is a reactive evaluation model.
 * It also includes a pool filter specifying how to filter out the pool of candidate tasks.
 */

public class ReactiveGPHHProblem extends GPProblem implements SimpleProblemForm {

    public static final String P_EVAL_MODEL = "eval-model";
    public static final String P_POOL_FILTER = "pool-filter";
    public static final String P_TIE_BREAKER = "tie-breaker";

    protected EvaluationModel evaluationModel;
    protected PoolFilter poolFilter;
    protected TieBreaker tieBreaker;


    /**
     * The number of times that the evaluate function is called. Cloning this object will reset
     * this field and therefore, cloning must be disabled with the
     * `eval.clone-problem = false`
     * line in the parameter file.
     */
    private static int evalCount = 0;


    /**
     * Gets the number of times that the <code>evaluate</code> method is invoked.
     * @return Number of times that the <code>evaluate</code> method is invoked
     */
    public static int getNumEvaluation()
    {
    	return evalCount;
    }

    public List<Objective> getObjectives() {
        return evaluationModel.getObjectives();
    }

    public EvaluationModel getEvaluationModel() {
        return evaluationModel;
    }

    public PoolFilter getPoolFilter() {
        return poolFilter;
    }

    public TieBreaker getTieBreaker() {
        return tieBreaker;
    }

    public void rotateEvaluationModel() {
        evaluationModel.rotateSeeds();
    }

    @Override
    public void setup(final EvolutionState state, final Parameter base) {
        super.setup(state, base);

        Parameter p = base.push(P_EVAL_MODEL);
        evaluationModel = (EvaluationModel)(
                state.parameters.getInstanceForParameter(
                        p, null, EvaluationModel.class));
        evaluationModel.setup(state, p);

        p = base.push(P_POOL_FILTER);
        poolFilter = (PoolFilter)(
                state.parameters.getInstanceForParameter(
                        p, null, PoolFilter.class));

        p = base.push(P_TIE_BREAKER);
        tieBreaker = (TieBreaker)(
                state.parameters.getInstanceForParameter(
                        p, null, TieBreaker.class));
    }

    @Override
    public void evaluate(EvolutionState state,
                         Individual indi,
                         int subpopulation,
                         int threadnum) {
        GPRoutingPolicy policy =
                new GPRoutingPolicy(poolFilter, ((GPIndividual)indi).trees[0]);

        // the evaluation model is reactive, so no plan is specified.
        long t = System.currentTimeMillis();
        evaluationModel.evaluate(policy, null, indi.fitness, state);
        t = System.currentTimeMillis() - t;
        ArrayList<DecisionSituation> seenDecicionSituations = evaluationModel.getSeenDecicionSituations();

        if(state instanceof DMSSaver) {
            DMSSaver gstate = (DMSSaver) state;
            boolean saveDMS = gstate.isDMSSavingEnabled();
            evaluationModel.setSaveDMSEnabled(saveDMS);

//        for(int i = 0; i < seenDecicionSituations.size(); i++)
//        {
//            ReactiveDecisionSituation rds = (ReactiveDecisionSituation)seenDecicionSituations.get(i);
//            if(rds.getState().getRemainingTasks().size() > 0)
//                continue;
//
//            if(i != seenDecicionSituations.size() - 1 &&
//                  ((ReactiveDecisionSituation)seenDecicionSituations.get(i+1)).getState().getRemainingTasks().size() != 0)
//            System.out.println(rds.getState().getSolution().toString());
//        }
//        final int numSeenSituations = seenDecicionSituations.size();
//        Collections.shuffle(seenDecicionSituations);
//        List<DecisionSituation> subList = seenDecicionSituations.subList(0, (numSeenSituations > 5) ? 5 : numSeenSituations);
//        List<DecisionSituation> sublist = new ArrayList<>(subList.size());
//        subList.forEach(item -> sublist.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) item)));
            if(saveDMS)
                gstate.updateSeenSituations(indi, seenDecicionSituations);
        }
        this.evaluationModel.resetSeenSituations();

        indi.evaluated = true;
        evalCount++;
//        indi.evalTime = t;
//        System.out.println(evalCount + ": " + ((GPIndividual)indi).trees[0].child.makeLispTree() + indi.fitness.fitness() + ", " + t);
    }
}
