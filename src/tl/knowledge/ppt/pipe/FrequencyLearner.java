package tl.knowledge.ppt.pipe;

import ec.EvolutionState;
import ec.gp.GPIndividual;

public class FrequencyLearner implements IPIPELearner<GPIndividual[]>
{

    /**
     * The name of all the GP terminals
     */
    private final String[] terminals;

    /**
     * The name of all the GP functions
     */
    private final String[] functions;

    /**
     * The evolution state object of ECJ.
     */
    private final EvolutionState state;

    /**
     * The ECJ thread that is running this learner.
     */
    private final int threadnum;

    /**
     * The learning rate. It is the parameter 'lr' in the paper.
     */
    private double lr;

    /**
     * Constructs a new instance.
     * @param state the {@code EvolutionState} that is governing this evolutionary process.
     * @param threadNum the index of the thread that is running this evolutionary process.
     * @param functions the name of all the GP functions.
     * @param terminals the name of all the GP terminals
     * @param learningRate the learning rate
     */
    public FrequencyLearner(EvolutionState state, int threadNum, String[] functions, String[] terminals, double learningRate)
    {
        this.state = state;
        this.threadnum = threadNum;
        this.functions = functions;
        this.terminals = terminals;
        this.lr = learningRate;
    }

    @Override
    public void adaptTowards(PPTree tree, GPIndividual[] individual, int treeIndex)
    {

    }

    @Override
    public void initialize(ProbabilityVector vector)
    {
        if(vector == null)
            throw new NullPointerException("The probability vector cannot be null.");

        if(vector.isInitialized())
            return;

        // TODO: How should 'R' be updated?
//        vector.R = state.random[this.threadnum].nextDouble();

        for(String tname : this.terminals)
            vector.probabilities.put(tname, 0d);

        for(String fname : this.functions)
            vector.probabilities.put(fname, 0d);

        vector.setInitialized();
    }
}
