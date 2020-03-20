package tl.gp.niching;


import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.ensemble.combiner.Aggregator;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class PhenoCharacterisation
{
    private RoutingPolicy rp;
    private RoutingPolicy referenceRule;
    private List<ReactiveDecisionSituation> decisionSituations;
    private int[] referenceIndexes;

    PhenoCharacterisation(final List<ReactiveDecisionSituation> dps, final RoutingPolicy referenceRule) {
        this.decisionSituations = dps;
        this.referenceRule = referenceRule;
        this.referenceIndexes = new int[this.decisionSituations.size()];
        this.calcReferenceIndexes();
    }

    public PhenoCharacterisation(final List<ReactiveDecisionSituation> dps) {
        this.decisionSituations = dps;
        this.referenceRule = null;
        this.referenceIndexes = new int[this.decisionSituations.size()];
    }

    public int[] characterise(final RoutingPolicy rule) {
        final int[] charList = new int[this.decisionSituations.size()];
        for (int i = 0; i < this.decisionSituations.size(); i++) {
            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
            final List<Arc> queue = (List<Arc>)situation.getPool();
            final int refIdx = this.referenceIndexes[i]; // Warning: This implementation does not consider the reference rule
            final Arc op = rule.next(situation);
            final int rank = situation.getPool().indexOf(op);
            charList[i] = rank;
        }
        return charList;
    }

    public List<int[]> cha(final RoutingPolicy rule)
    {
        List<int[]> retval = new ArrayList<>();
        for (int i = 0; i < decisionSituations.size(); i++)
        {
            ReactiveDecisionSituation situation = decisionSituations.get(i);
            List<Arc> unservedTasks = situation.getPool();
            PriorityQueue<Pair<Arc, Double>> taskPriorities = new PriorityQueue<>(Comparator.comparingDouble(Pair::getValue));
            for (Arc task : unservedTasks)
            {
                double priority = rule.priority(task, situation.getRoute(), situation.getState());
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

    public int[] characterise(ArrayList<GPRoutingPolicy> rules)
    {
        final int[] charList = new int[this.decisionSituations.size()];
        Aggregator agg = new Aggregator();
        for (int i = 0; i < this.decisionSituations.size(); i++)
        {
            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
            Arc op = agg.next(situation, rules, SimpleTieBreaker.getInstance());
            int rank = situation.getPool().indexOf(op);
            charList[i] = rank;
        }

        return charList;
    }

//    private int[] characterise(RoutingPolicy[] rules, ReactiveDecisionSituation situation)
//    {
////        List<Arc> pool = situation.getPool();
////        NodeSeqRoute routes = situation.getRoute();
////        DecisionProcessState state = situation.getState();
////        EnsemblePolicy ensemble = new EnsemblePolicy()
//
//        Aggregator agg = new Aggregator();
//        List<Double> priorities = agg.priority(situation, rules);
//        List<Double> sortedPriorities = priorities.stream().sorted().collect(Collectors.toList());
//
//        int[] charList = new int[priorities.size()];
//        for(int i =0; i < sortedPriorities.size(); i++)
//        {
//            charList[i] = priorities.indexOf(sortedPriorities.get(i));
//        }
//
//        return charList;
//    }
//
//    public double distance(RoutingPolicy[] rules, ReactiveDecisionSituation rds)
//    {
//        return distance()
//    }

//    private ArrayList<Double> priCharacterise(RoutingPolicy rule)
//    {
//        ArrayList<Double> charList = new ArrayList<>(this.decisionSituations.size());
//        for (int i = 0; i < this.decisionSituations.size(); i++) {
//            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
//            final List<Arc> queue = (List<Arc>)situation.getPool();
//            final int refIdx = this.referenceIndexes[i]; // Warning: This implementation does not consider the reference rule
//            final Arc op = rule.next(situation);
//            charList.add(op.getPriority());
//        }
//        return charList;
//    }
//
//    public double priorityCharacterise(RoutingPolicy[] rules)
//    {
//        ArrayList<Double> aggregatedPriorities = new ArrayList<>(decisionSituations.size());
//        for(RoutingPolicy rule : rules)
//        {
//            ArrayList<Double> priorities = new ArrayList<>();
//            SummaryStatistics priorityStatistics = new SummaryStatistics();
//            for (ReactiveDecisionSituation situation : this.decisionSituations)
//            {
//                Arc next = rule.next(new ReactiveDecisionSituation(situation));
//                priorities.add(next.getPriority());
//                priorityStatistics.addValue(next.getPriority());
//            }
//            for (int i = 0; i < priorities.size(); i++)
//            {
//                double priority = priorities.get(i);
//                priority = (priority - priorityStatistics.getMin()) / (priorityStatistics.getMax() - priorityStatistics.getMax());
//                priorities.set(i, priority);
//            }
//            for(int i = 0; i < this.decisionSituations.size(); i++)
//                aggregatedPriorities.set(i, priorities.stream().mapToDouble(d -> d).sum());
//        }
//
//        Collections.max(aggregatedPriorities);
//    }

    public static double distance(final int[] charList1, final int[] charList2) {
        double distance = 0.0;
        for (int i = 0; i < charList1.length; ++i) {
            final double diff = charList1[i] - charList2[i];
            distance += diff * diff;
        }
        return Math.sqrt(distance);
    }

    private void calcReferenceIndexes() {
        for (int i = 0; i < this.decisionSituations.size(); ++i) {
            final ReactiveDecisionSituation situation = this.decisionSituations.get(i);
            final Arc op = this.referenceRule.next(situation);
            final int index = situation.getPool().indexOf(op);
            this.referenceIndexes[i] = index;
        }
    }
}
