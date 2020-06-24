package tl.gp.characterisation;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;

import java.util.List;

/**
 * This characterisation method makes its decision based on a list of decision situations. For each situation, S, it
 * considers the set of tasks, TASKS, inside the S, asks the routing policy to select one of TASKS and finds the
 * index of the selected task inside TASKS and considers that as the characterisation of the rule for the situation S.
 *
 * This is based on the code that Shaolin gave me. Although it does not make much sense, the Euclidean distance measure
 * based on this methods and the surrogate method based on it work very well.
 */
public class TaskIndexCharacterisation implements RuleCharacterisation<int[]>
{
	private final List<ReactiveDecisionSituation> decisionSituations;

//	FileUtils fu = new FileUtils("ti");

	public TaskIndexCharacterisation(final List<ReactiveDecisionSituation> dps)
	{
		if(dps == null || dps.isEmpty())
			throw new IllegalArgumentException("List of decision situations cannot be null or empty: " + dps);
		this.decisionSituations = dps;
	}

	public int[] characterise(GPRoutingPolicy rule)
	{
//		fu.println(rule.getGPTree().child.makeLispTree());
		int dsSize = this.decisionSituations.size();
		final int[] charList = new int[dsSize];
		for (int i = 0; i < dsSize; i++) {
			final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
//			if(rule.getGPTree().child.makeLispTree()
//					.equalsIgnoreCase("(max (max (* (max CFD (+ FRT CFH)) (- RQ DC)) (* (min (* CFD DC) DEM) (max (min (- SC CTD) (* FUT (* FUT SC))) CFH))) (* (max CFD (max (max (max 0.8416072097232807 CFH) (min DEM DEM)) (max (min (* FUT SC) (+ CFH DEM)) (* CTT1 (* RQ CTD))))) (- RQ DC)))"))
//			{
//				final int j = i;
//				if(j == 6)
//				{
//					fu.println("Instance: " + situation.getState().getInstance().toString());
//				}
//				fu.println("Pool: ");
//				situation.getPool().forEach(a -> fu.print(j + ": " + a.toString()));
//				fu.println("Route: " + situation.getRoute().toString() + ", current node: " + situation.getRoute().currNode());
//			}
			final Arc op = rule.next(situation);
			final int index = situation.getPool().indexOf(op);
//			if(rule.getGPTree().child.makeLispTree().equalsIgnoreCase("(max (- RQ CFH) (* (max CFD (max (max (max 0.8416072097232807 (- SC CTD)) CFD) (max (min (- RQ CFH) (* CFD DC)) (* CTT1 (* RQ CTD))))) (min (max (max (+ CFH DEM) (- SC CTD)) (* CTT1 RQ)) (* (max CFD (- SC CTD)) (- RQ DC)))))"))
//			{
//				fu.println("Selected: " + index + ", " + (op != null ? op.toString() + "pr: " + op.getPriority() + "\n" : "null"));
//			}
			charList[i] = index;
		}
//		fu.println(Arrays.toString(charList));
//		fu.println();
		return charList;
	}
}
