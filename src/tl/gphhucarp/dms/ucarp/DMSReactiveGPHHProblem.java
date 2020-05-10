package tl.gphhucarp.dms.ucarp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.ReactiveGPHHProblem;
import gphhucarp.gp.evaluation.EvaluationModel;
import tl.gphhucarp.dms.DMSSavingGPHHState;

import java.util.ArrayList;
import java.util.List;

/**
 * A reactive GPHH problem to evaluate a reactive routing policy during the GPHH.
 * The evaluationg model is a reactive evaluation model.
 * It also includes a pool filter specifying how to filter out the pool of candidate tasks.
 */

public class DMSReactiveGPHHProblem extends ReactiveGPHHProblem
{
	/**
	 * The number of times that the evaluate function is called. Cloning this object will reset
	 * this field and therefore, cloning must be disabled with the
	 * `eval.clone-problem = false`
	 * line in the parameter file.
	 */
	private static int evalCount = 0;

	DMSEvaluationModel evaluationModel;


	/**
	 * Gets the number of times that the <code>evaluate</code> method is invoked.
	 * @return Number of times that the <code>evaluate</code> method is invoked
	 */
	public static int getNumEvaluation()
	{
		return evalCount;
	}

	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base);

		Parameter p = base.push(P_EVAL_MODEL);
		evaluationModel = (DMSEvaluationModel)(
				state.parameters.getInstanceForParameter(
						p, null, EvaluationModel.class));
		evaluationModel.setup(state, p);
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

        if(state instanceof DMSSavingGPHHState)
        {
            DMSSavingGPHHState gstate = (DMSSavingGPHHState) state;
            gstate.updateSeenSituations(indi, seenDecicionSituations);
        }

        this.evaluationModel.resetSeenSituations();

		indi.evaluated = true;
		evalCount++;
//		indi.evalTime = t;
	}
}
