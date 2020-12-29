package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.TieBreaker;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.evaluation.EvaluationModel;
import tl.gp.KnowledgeableProblemForm;

import java.util.ArrayList;

public class MultiPopReactiveGPHHProblem extends GPProblem implements SimpleProblemForm, KnowledgeableProblemForm
{
	public static final String P_EVAL_MODEL = "eval-model";
	public static final String P_POOL_FILTER = "pool-filter";
	public static final String P_TIE_BREAKER = "tie-breaker";

	protected EvaluationModel[] evaluationModel;
	protected PoolFilter poolFilter;
	protected TieBreaker tieBreaker;

	/**
	 * The number of times that the evaluate function is called. Cloning this object will reset
	 * this field and therefore, cloning must be disabled with the
	 * `eval.clone-problem = false`
	 * line in the parameter file.
	 */
	private static int evalCount = 0;

	public void rotateEvaluationModel()
	{
		for(EvaluationModel model : evaluationModel)
			model.rotateSeeds();
	}

	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base);

		int nSubPop = state.parameters.getInt(new Parameter("pop.subpops"), null);
		evaluationModel = new EvaluationModel[nSubPop];
		Parameter p = base.push(P_POOL_FILTER);
		poolFilter = (PoolFilter)(
				state.parameters.getInstanceForParameter(
						p, null, PoolFilter.class));

		p = base.push(P_TIE_BREAKER);
		tieBreaker = (TieBreaker)(
				state.parameters.getInstanceForParameter(
						p, null, TieBreaker.class));

		for (int subpop = 0; subpop < nSubPop; subpop++)
		{
			p = base.push("subpop").push("" + subpop).push(P_EVAL_MODEL);
			evaluationModel[subpop] = (EvaluationModel)(
					state.parameters.getInstanceForParameter(
							p, null, EvaluationModel.class));
			evaluationModel[subpop].setup(state, p);
		}
	}

	@Override
	public void evaluate(EvolutionState state, Individual indi, int subpopulation, int threadnum)
	{
		GPRoutingPolicy policy =
				new GPRoutingPolicy(poolFilter, ((GPIndividual)indi).trees[0]);

		// the evaluation model is reactive, so no plan is specified.
		evaluationModel[subpopulation].evaluate(policy, null, indi.fitness, state);
		ArrayList<DecisionSituation> seenDecicionSituations = evaluationModel[subpopulation].getSeenDecicionSituations();

		if(state instanceof MultiPopDMSSaver)
		{
			MultiPopDMSSaver gstate = (MultiPopDMSSaver) state;
			boolean saveDMS = gstate.isDMSSavingEnabled();
			evaluationModel[subpopulation].setSaveDMSEnabled(saveDMS);

			if(saveDMS)
				gstate.updateSeenSituations(subpopulation, indi, seenDecicionSituations);
		}
		this.evaluationModel[subpopulation].resetSeenSituations();

		indi.evaluated = true;
		evalCount++;
	}

	public PoolFilter getPoolFilter()
	{
		return poolFilter;
	}

	@Override
	public int getEvalCount() {
		return evalCount;
	}
}
