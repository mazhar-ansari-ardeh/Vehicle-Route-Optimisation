package tl.knowledge.ppt.pipe;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.gp.GPIndividualUtils;

import java.util.Map;

/**
 * This class implements the basic learning method of PPTree models that is presented in the paper: <br/>
 * "R.P. Salustowicz, J. Schmidhüber, Probabilistic incremental program evolution. Evol. Comput. 5(2), 123–141 (1997)".
 */
class PIPELearner
{
    /**
     * The probability of selecting an instruction from the terminal set.
     */
    private final double PT;

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
     * A user-defined constant. This constant determines the degree of 'fitness dependent learning' influence.
     */
    private double epsilon;

    /**
     * The best program seen so far.
     */
    private GPIndividual elite = null;

    /**
     * According to the paper:
     * 'clr is a constant influencing the number of iterations. We use clr = 0.1, which turned out to be a good
     * compromise between precision and speed'.
     */
    private double clr = 0.1;


    /**
     * Constructs a new instance.
     * @param PT the probability of selecting an instruction from the terminal set.
     * @param functions the name of all the GP functions.
     * @param terminals the name of all the GP terminals
     * @param learningRate the learning rate
     * @param epsilon the user-defined parameter epsilon
     */
    public PIPELearner(EvolutionState state, int threadnum, double PT , String[] functions, String[] terminals,
                       double learningRate, double epsilon)
    {
        this.state = state;
        this.threadnum = threadnum;
        this.PT = PT;
        this.functions = functions;
        this.terminals = terminals;
        this.lr = learningRate;
        this.epsilon = epsilon;
    }

    /**
     * Calculates the target probability that the learning algorithm should try to achieve.
     * @param probabilityOfBest the probability with which the best individual may appear from the probability model.
     * @param fitnessOfBest fitness of the best individual in the current generation.
     * @param fitnessOfElite fitness of the best individual seen so far.
     * @return The probability to target for.
     */
    private double targetProbability(double probabilityOfBest, double fitnessOfBest, double fitnessOfElite)
    {
        double retval = probabilityOfBest;
        retval = retval + (1 - probabilityOfBest)*lr*((epsilon + fitnessOfElite)/(epsilon + fitnessOfBest));
        return retval;
    }

    /**
     * Adapt the probability model towards the given individual.
     * @param tree the probability model tree to update.
     * @param treeIndex The GP tree to consider in the given GP individual
     * @param individual the individual to learn from.
     */
    public void adaptTowards(PPTree tree, GPIndividual individual, int treeIndex)
    {
        if(elite == null)
            elite = individual;

        double probabilityOfInd = tree.probabilityOf(individual, treeIndex);
        double targetProbability = targetProbability(probabilityOfInd, individual.fitness.fitness(),
                                                     elite.fitness.fitness());
        GPNode root = individual.trees[treeIndex].child;
        while(targetProbability <= probabilityOfInd)
        {
            Map<String, GPNode> index = GPIndividualUtils.index(individual.trees[treeIndex].child);
            for(String address : index.keySet())
            {
                GPNode node = index.get(address);
                double oldItemProb = tree.getProbabilityOf(address, node.name());
                double newItemProb = oldItemProb + clr*lr*(1 - oldItemProb);
                if(newItemProb < 0 || newItemProb > 1)
                {
                    System.err.println("New probability is not valid: " + newItemProb);
                    System.err.println("Node: " + node.name() + ", oldItemProb: " + oldItemProb);
                    System.err.println("clr: " + clr + ", lr: " + lr);
                    System.err.println("The probability is ignored.");
                    continue;
                }
                tree.setProbabilityOf(address,node.name(), newItemProb);
                if(node.name().equals("ERP")) // TODO: 28/08/19 This is incorrect.
                    tree.setR(address, Double.valueOf(node.toString()));
            }
        }

        // TODO: 28/08/19 Should this be done first?
        if(elite == null || (individual.fitness.fitness() < elite.fitness.fitness()))
            elite = individual;
    }

    public void initialize(ProbabilityVector vector)
    {
        if(vector.isInitialized())
            return;

        vector.R = state.random[this.threadnum].nextDouble();

        int tlen = terminals.length;
        int flen = functions.length;

        for(String tname : this.terminals)
            vector.probabilities.put(tname, PT / tlen);

        for(String fname : this.functions)
            vector.probabilities.put(fname, (1 - PT)/flen);

        vector.setInitialized();
    }
}
