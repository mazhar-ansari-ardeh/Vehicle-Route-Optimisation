package tl.gphhucarp;

import ec.Evaluator;
import ec.EvolutionState;
import ec.Evolve;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.ReactiveGPHHProblem;
import gphhucarp.gp.UCARPPrimitiveSet;
import gphhucarp.gp.evaluation.EvaluationModel;
import gputils.LispUtils;


/**
 * The main program of the GP test process.
 * It reads the out.stat files from the training path subject to the solution and fitness types.
 * Then it tests all the solutions read from the training files on the test set.
 * Finally, it writes all the related information to a csv file.
 */

public class EvaluateIndividual
{
    public static void main(String[] args) {
        ParameterDatabase parameters = Evolve.loadParameterDatabase(new String[]{
                "-file", "/home/mazhar/grid/carp_param_base.param",
                "-p", "generations=200",
                "-p", "eval.problem.eval-model.instances.0.vehicles=11",
                "-p", "eval.problem.eval-model.instances.0.file=gdb/gdb8.dat",
                "-p", "eval.problem.eval-model.instances.0.samples=5",
                "-p", "seed.0=25556"
        });

//        ECJUtils.loadECJ()

        EvolutionState state = Evolve.initialize(parameters, 0);

        Parameter p;

        // setup the evaluator, essentially the test evaluation model
        p = new Parameter(EvolutionState.P_EVALUATOR);
        state.evaluator = (Evaluator)
                (parameters.getInstanceForParameter(p, null, Evaluator.class));
        state.evaluator.setup(state, p);

        p = new Parameter("eval.problem.eval-model.instances.0.file");
        System.out.println("Dataset: " + state.parameters.getString(p, null));

        p = new Parameter("eval.problem.eval-model.instances.0.vehicles");
        System.out.println("Num-vehicles: " + state.parameters.getInt(p, null));

        p = new Parameter("eval.problem.eval-model.instances.0.samples");
        System.out.println("Samples: " + state.parameters.getInt(p, null));
//
//
//        p = new Parameter("eval.problem.eval-model.instances.0.samples");
//        state.parameters.set(p, "5"); // Training dataset.
//        System.out.println(state.parameters.getInt(p, null));

        gphhucarp.gp.ReactiveGPHHProblem testProblem = (ReactiveGPHHProblem)state.evaluator.p_problem;
        EvaluationModel testEvaluationModel = testProblem.getEvaluationModel();

        String expression = "(/ (- (+ (- (* CFH CR) (min (min CFD (* 0.4393670032186131 CR)) (* CFH CR))) (max DEM1 (min (min CR DEM) SC))) (/ (min (* (max FUT CR) (max DEM1 (max DEM1 CTT1))) (max (+ (+ CFR1 CR) (+ CFR1 CR)) (+ (min RQ CFD) 0.213575008343024))) (/ (- (+ SC (- DC CFD)) (max 0.6778818234475281 DC)) (/ (* 0.4393670032186131 CR) CFH)))) (max (- (* 0.4393670032186131 CR) 0.4393670032186131) (- (- (+ SC (- DC CFD)) (max 0.15794104802197617 (min CR (min RQ CFD)))) (+ SC (- DC CFD)))))";
        expression = LispUtils.simplifyExpression(expression);
        RoutingPolicy routingPolicy = new GPRoutingPolicy(testProblem.getPoolFilter(), LispUtils.parseExpression(expression,
                                            UCARPPrimitiveSet.wholePrimitiveSet()));

        double fitness = -1;
        MultiObjectiveFitness f = new MultiObjectiveFitness();
        f.objectives = new double[1];
        f.objectives[0] = fitness;

        // test the rules for each generation
        long start = System.currentTimeMillis();

        testEvaluationModel.evaluateOriginal(routingPolicy, null, f, state);
        System.out.println("Fitness: " + f.fitness());

        long finish = System.currentTimeMillis();
        long duration = finish - start;
        System.out.println("Duration = " + duration + " ms.");
    }
}
