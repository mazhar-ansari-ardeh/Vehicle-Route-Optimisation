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

    public static final String CMP_ORIGIN = "cmp-ppt-breeding";

    /**
     * The probability that this pipeline samples from the complement of the PPT rather than the PPT itself.
     */
    public static final String P_COMPLEMENT_PROB = "complement-probability";
    private double cmpProb;

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

        Parameter p = base.push(P_COMPLEMENT_PROB);
        cmpProb = state.parameters.getDoubleWithDefault(p, null, 0);
        log(state, logID, "Probability of using PPT complement: " + cmpProb);
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
        PPTree cmpTree = tree.complement(true);

        int n = typicalIndsProduced();
        if (n < min) n = min;
        if (n > max) n = max;

        for(int q=start; q < n+start; q++)
        {
            GPNode root;
            String origin;
            if(state.random[thread].nextDouble() < cmpProb)
            {
                origin = CMP_ORIGIN;
                do
                {
                    root = cmpTree.sampleIndividual(state.random[thread]);
                }while (root.depth() < 4); // TODO: Don't use a magic number here.
                log(state, logID, "Gen: " + state.generation + "\nSampled from the complement: " + root.makeGraphvizTree() + "\n");
            } else
            {
                origin = ORIGIN;
                root = tree.sampleIndividual(state.random[thread]);
            }
            Fitness fitness = (Fitness)state.population.subpops[subpopulation].species.f_prototype.clone();
            TLGPIndividual ind = GPIndividualUtils.asGPIndividual(root);
            ind.fitness = fitness;
            ind.setOrigin(origin);
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
