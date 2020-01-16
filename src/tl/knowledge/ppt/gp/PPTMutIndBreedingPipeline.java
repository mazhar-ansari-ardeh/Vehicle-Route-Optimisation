package tl.knowledge.ppt.gp;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.gp.GPBreedingPipeline;
import ec.gp.GPNode;
import ec.gp.GPNodeBuilder;
import ec.gp.GPNodeSelector;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.Mutator;
import tl.gp.PPTEvolutionState;
import tl.gp.TLGPIndividual;
import tl.knowledge.ppt.pipe.PPTree;


public class PPTMutIndBreedingPipeline extends GPBreedingPipeline implements TLLogger<GPNode>
{
    public static final int NUM_SOURCES = 0;

    public static final String ORIGIN = "ppt-breeding";

    public static final String MUT_ORIGIN = "ppt-mut-ind-breeding";

    /**
     * Number of evaluations that this class has performed.
     */
    private static int numEvals = 0;

    /**
     * The probability that this pipeline samples from the complement of the PPT rather than the PPT itself.
     */
//    public static final String P_MUTATAION_PROB = "mutation-probability";
////    private double mutProb;

    private int logID;
    private GPNodeBuilder builder;
    private GPNodeSelector nodeselect;

    @Override
    public int numSources()
    {
        return NUM_SOURCES;
    }

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        logID = setupLogger(state, base);

//        Parameter p = base.push(P_MUTATAION_PROB);
//        mutProb = state.parameters.getDoubleWithDefault(p, null, 0);
//        log(state, logID, "Probability of using PPT complement: " + mutProb);

        Parameter p = base.push("mut-builder");
        builder = (GPNodeBuilder)(state.parameters.getInstanceForParameter(p, /*d*/ null, GPNodeBuilder.class));
        builder.setup(state,p);
        builder.maxSize = 8;

        p = base.push("mut-selector");
        nodeselect = (GPNodeSelector)(state.parameters.getInstanceForParameter(p, null /*d*/, GPNodeSelector.class));
        nodeselect.setup(state,p);
    }

    @Override
    public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread)
    {
        if (!(state instanceof PPTEvolutionState))
        {
            throw new RuntimeException("state must be of type PPTEvolutionState");
        }

        PPTree tree = ((PPTEvolutionState) state).getPpt();
        if(tree == null)
            throw new RuntimeException("PPT cannot be null.");
//        PPTree cmpTree = tree.complement(true);

        int n = typicalIndsProduced();
        if (n < min) n = min;
        if (n > max) n = max;
        Mutator mutator = new Mutator(state, nodeselect, builder, 3, 8, -1, false);
        for(int q=start; q < n+start; q++)
        {
            String origin = ORIGIN;

            GPNode root;
            root = tree.sampleIndividual(state.random[thread]);
            log(state, logID, "Gen: " + state.generation + "\nSampled from the PPT:\n" + root.makeGraphvizTree() + "\n");
            Fitness fitness = (Fitness)state.population.subpops[subpopulation].species.f_prototype.clone();
            TLGPIndividual ind = GPIndividualUtils.asGPIndividual(root, fitness);
            double prob = tree.probabilityOf(ind, 0, false);
            ind.setOrigin(origin);

            GPNode mutatedRoot = mutator.mutate(subpopulation, root, state, thread);
            Fitness fitness2 = (Fitness)state.population.subpops[subpopulation].species.f_prototype.clone();
            TLGPIndividual mutatedInd = GPIndividualUtils.asGPIndividual(mutatedRoot, fitness2);
            mutatedInd.setOrigin(origin);
            double probMutated = tree.probabilityOf(mutatedInd, 0, false);
            mutatedInd.setOrigin(MUT_ORIGIN);
            if(probMutated <= prob)
            {
                ((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, mutatedInd, subpopulation, thread);
                ((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, subpopulation, thread);
                numEvals += 2;
                if(mutatedInd.fitness.fitness() <= ind.fitness.fitness())
                {
                    log(state, logID, "Gen: " + state.generation + "\nMutated from the PPT:\n" + mutatedRoot.makeGraphvizTree() + "\n");
                    log(state, logID, "Mutated fitness: " + mutatedInd.fitness.fitness() + ", original fitness: " + ind.fitness.fitness() + "\n\n");
                    log(state, logID, "Number of evaluations: " + numEvals);
                    ind = mutatedInd;
                }
            }

            inds[q] = ind;
//            System.out.println(root.makeGraphvizTree() + "\n" + "prob: " + prob + "\n" + ind.fitness.fitness() + "\n\n");
//            System.out.println(mutatedRoot.makeGraphvizTree() + "\n" + "prob: " + prob + "\n" + mutatedInd.fitness.fitness() + "\n\n");
//            log(state, logID, "Gen:\t" + state.generation + ",\n + Ind: " + root.makeGraphvizTree() + "\n\n");
        }
        return n;
    }

    @Override
    public Parameter defaultBase()
    {
        return new Parameter("ppt-mut-ind-breed");
    }

    @Override
    public int typicalIndsProduced()
    {
        return 1;
    }

    @Override
    public int minChildProduction()
    {
        return 1;
    }

    @Override
    public int maxChildProduction()
    {
        return 1;
    }
}
