package tl.gp.characterisation;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** This characterisation method makes its decision based on a list of decision situations. For each situation, S, it
* considers the set of tasks, TASKS, inside the S, asks the routing policy to calculate the priority of each task in
 * TASKS and then ranks each task in TASKS based on the priority value and returns the array of ranks as the
 * characterisation of the rule for task S.
 *
 * This method is based on the discussion I had with Yi.
*/
public class TaskRankCharacterisation implements RuleCharacterisation<List<int[]>>
{
	private final List<ReactiveDecisionSituation> decisionSituations;

	public TaskRankCharacterisation(final List<ReactiveDecisionSituation> dps)
	{
		if(dps == null || dps.isEmpty())
			throw new IllegalArgumentException("List of decision situations cannot be null or empty: " + dps);
		this.decisionSituations = dps;
	}

	@Override
	public List<int[]> characterise(GPRoutingPolicy policy)
	{
		List<int[]> retval = new ArrayList<>();
		for (int i = 0; i < decisionSituations.size(); i++)
		{
			ReactiveDecisionSituation situation = decisionSituations.get(i);
			List<Arc> unservedTasks = situation.getPool();
			PriorityQueue<Pair<Arc, Double>> taskPriorities = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));
			for (Arc task : unservedTasks)
			{
				double priority = policy.priority(task, situation.getRoute(), situation.getState());
				taskPriorities.add(new ImmutablePair<>(task, priority));
			}
			int[] ranks = new int[taskPriorities.size()];
			int rank = 1;
			while(!taskPriorities.isEmpty())
			{
				Pair<Arc, Double> aPair = taskPriorities.poll();
				ranks[unservedTasks.indexOf(aPair.getKey())] = rank++;
			}

			retval.add(ranks);
		}

		return retval;
	}
}
