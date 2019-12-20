package tl.knowledge.ppt.gp;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.gp.GPBreedingPipeline;
import ec.gp.GPNode;
import ec.util.Parameter;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.PPTEvolutionState;
import tl.gp.TLGPIndividual;
import tl.knowledge.ppt.pipe.PPTree;

public class PPTBreedingPipeline extends GPBreedingPipeline implements TLLogger<GPNode>
{
    public static final int NUM_SOURCES = 0;

    public static final String ORIGIN = "ppt-breeding";

    private int logID;

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

        int n = typicalIndsProduced();
        if (n < min) n = min;
        if (n > max) n = max;

        for(int q=start; q < n+start; q++)
        {
            GPNode root = tree.sampleIndividual(state.random[thread]);
            Fitness fitness = (Fitness)state.population.subpops[subpopulation].species.f_prototype.clone();
            TLGPIndividual ind = GPIndividualUtils.asGPIndividual(root);
            ind.fitness = fitness;
            ind.setOrigin(ORIGIN);
            inds[q] = ind;
//            log(state, logID, "Gen:\t" + state.generation + ",\n + Ind: " + root.makeGraphvizTree() + "\n\n");
        }
        return n;
    }

    @Override
    public Parameter defaultBase()
    {
        return new Parameter("pptbreed");
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
