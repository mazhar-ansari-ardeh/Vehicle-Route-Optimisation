package tl.knowledge.surrogate.lsh;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.evaluation.EvaluationModel;
import gphhucarp.gp.evaluation.ReactiveEvaluationModel;
import gphhucarp.representation.Solution;
import gphhucarp.representation.route.TaskSeqRoute;

public class SurrogateEvaluationModel extends EvaluationModel
{
    LSHSurrogate surrogate;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
    }

    @Override
    public void evaluate(RoutingPolicy policy, Solution<TaskSeqRoute> plan, Fitness fitness, EvolutionState state)
    {
        double[] fitnesses = new double[1];
        fitnesses[0] = surrogate.fitness((GPRoutingPolicy) policy);
        MultiObjectiveFitness f = (MultiObjectiveFitness)fitness;
        f.setObjectives(state, fitnesses);
    }

    @Override
    public void evaluateOriginal(RoutingPolicy policy, Solution<TaskSeqRoute> plan, Fitness fitness, EvolutionState state)
    {
        throw new RuntimeException("This method is not supported.");
    }
}
