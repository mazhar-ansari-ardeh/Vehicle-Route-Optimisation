package tl.gphhucarp.dms.ucarp;

import ec.EvolutionState;
import ec.util.Parameter;
import gphhucarp.core.InstanceSamples;
import gphhucarp.core.Objective;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.gp.evaluation.EvaluationModel;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.NodeSeqRoute;
import org.apache.commons.lang3.tuple.Pair;
import tl.gphhucarp.dms.DMSSaver;

import java.util.ArrayList;

/**
 * The evaluation model for evaluating individuals in GPHH.
 */

public abstract class DMSEvaluationModel extends EvaluationModel
{

	/**
	 * The list of the decision situations that this evaluation model encountered during the evaluation of a routing policy.
	 */
	private ArrayList<DecisionSituation> seenDecicionSituations = new ArrayList<>();

	public ArrayList<DecisionSituation> getSeenDecicionSituations()
	{
		return seenDecicionSituations;
	}

	public void updateSeenDecicionSituations(ArrayList<DecisionSituation> seenDecicionSituations)
	{
		if(this.seenDecicionSituations.size() > 2000)
			return;
		this.seenDecicionSituations.addAll(seenDecicionSituations);
	}

	public void resetSeenSituations()
	{
		this.seenDecicionSituations.clear();
	}

	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);

		if(state instanceof DMSSaver)
		{
			seenDecicionSituations.forEach(((DMSSaver) state)::addToInitialSituations);
			seenDecicionSituations.clear(); // This was added when I tried to
		}
	}

	public void calcObjRefValueMap() {
		int index = 0;
		ArrayList<DecisionSituation> situations = new ArrayList<>();
		for (InstanceSamples iSamples : instanceSamples) {
			for (long seed : iSamples.getSeeds()) {
				// create a new reactive decision process from the based intance and the seed.
				DMSReactiveDecisionProcess dp =
						DMSDecisionProcess.initDMSReactive(iSamples.getBaseInstance(),
								seed, Objective.refReactiveRoutingPolicy());

				// get the objective reference values by applying the reference routing policy.
				dp.run();
                updateSeenDecicionSituations(dp.getSeenSituations());
				Solution<NodeSeqRoute> solution = dp.getState().getSolution();
				for (Objective objective : objectives) {
					double objValue = solution.objValue(objective);
					objRefValueMap.put(Pair.of(index, objective), objValue);
					index ++;
				}
				dp.reset();
			}
		}
	}
}
