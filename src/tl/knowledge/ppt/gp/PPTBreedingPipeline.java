package tl.knowledge.ppt.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPBreedingPipeline;
import ec.util.Parameter;
import tl.knowledge.ppt.pipe.IPIPELearner;
import tl.knowledge.ppt.pipe.PPTree;

public class PPTBreedingPipeline extends GPBreedingPipeline
{
    /**
     * The probability prototype tree that will be used to sample new individuals.
     */
    private final PPTree tree;


//    /**
//     * The learning rate for learning.
//     */
//    private double lr;
//    public static final String P_LEARNING_RATE = "lr";

    public PPTBreedingPipeline(PPTree tree)
    {
        if(tree == null)
            throw new IllegalArgumentException("PPT cannot be null.");
        this.tree = tree;
    }

    @Override
    public int numSources()
    {
        return 0;
    }

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
    }

    @Override
    public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread)
    {
        return 0;
    }

    @Override
    public Parameter defaultBase()
    {
        return new Parameter("pptbreed");
    }
}
