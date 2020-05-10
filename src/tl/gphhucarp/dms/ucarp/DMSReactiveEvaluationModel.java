package tl.gphhucarp.dms.ucarp;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.core.InstanceSamples;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionProcess;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionProcess;
import gphhucarp.gp.evaluation.ReactiveEvaluationModel;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.representation.route.TaskSeqRoute;

import java.util.ArrayList;


public class DMSReactiveEvaluationModel  extends DMSEvaluationModel
{
	@Override
	public void evaluate(RoutingPolicy policy, Solution<TaskSeqRoute> plan,
						 Fitness fitness, EvolutionState state) {
		double[] fitnesses = new double[objectives.size()];

		int numdps = 0;
		for (InstanceSamples iSamples : instanceSamples) {
			for (long seed : iSamples.getSeeds()) {
				// create a new reactive decision process from the based intance and the seed.
				DMSReactiveDecisionProcess dp =
						(DMSReactiveDecisionProcess) DMSDecisionProcess.initDMSReactive(iSamples.getBaseInstance(),
								seed, policy);

				dp.run();
				ArrayList<DecisionSituation> seenSituations = dp.getSeenSituations();
				updateSeenDecicionSituations(seenSituations);
				seenSituations.clear();

				Solution<NodeSeqRoute> solution = dp.getState().getSolution();
				for (int j = 0; j < fitnesses.length; j++) {
					Objective objective = objectives.get(j);
					double normObjValue =
							solution.objValue(objective); // / getObjRefValue(i, objective);
					fitnesses[j] += normObjValue;
				}
				dp.reset();

				numdps ++;
			}
		}

		for (int j = 0; j < fitnesses.length; j++) {
			fitnesses[j] /= numdps;
		}

		MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
		f.setObjectives(state, fitnesses);
	}

	@Override
	public void evaluateOriginal(RoutingPolicy policy,
								 Solution<TaskSeqRoute> plan,
								 Fitness fitness, EvolutionState state) {
		double[] fitnesses = new double[objectives.size()];

		int numdps = 0;
		for (InstanceSamples iSamples : instanceSamples) {
			for (long seed : iSamples.getSeeds()) {
				// create a new reactive decision process from the based intance and the seed.
				ReactiveDecisionProcess dp =
						DecisionProcess.initReactive(iSamples.getBaseInstance(),
								seed, policy);

				dp.run();
				Solution<NodeSeqRoute> solution = dp.getState().getSolution();
				for (int j = 0; j < fitnesses.length; j++) {
					Objective objective = objectives.get(j);
					double normObjValue =
							solution.objValue(objective);
					fitnesses[j] += normObjValue;
				}
				dp.reset();

				numdps ++;
			}
		}

		for (int j = 0; j < fitnesses.length; j++) {
			fitnesses[j] /= numdps;
		}

		MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
		f.setObjectives(state, fitnesses);
	}
}
