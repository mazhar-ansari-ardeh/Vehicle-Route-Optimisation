package tl.knowledge.ppt;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.collections.tree.Tree;

import java.util.*;


class PPTProbabilityVector
{
    /**
     * The structure that maps terminal/function name to its probability.
     */
    HashMap<String, Double> probabilities = new HashMap<>();

    /**
     * When this probability vector is used to sample a node, if ERC terminal is selected, then the value of the node will be
     * generated randomly, unless the probability of ERC is greater that a threshold TR. In this case, the value in this
     * field will be used.
     */
    double R;

    /**
     * List of all the terminal names (including ERC) that can appear in GP system.
     */
    private String[] terminals;

    /**
     * List of all the function names that can appear in GP system.
     */
    private String[] functions;

    private boolean initialized = false;

    /**
     * Checks if the PPTree is initialized for learning or not. Being initialized is the first step towards learning a PPT.
     * @return {@code true} if the tree is initialized.
     */
    public boolean isInitialized()
    {
        return this.initialized;
    }

    /**
     * Sets the state of this node as initialized. Once a node is initialised, it cannot be reset.
     */
    public void setInitialized()
    {
        this.initialized = true;
    }
}

/**
 * This class implements the Probabilistic Prototype Tree data structure introduced in the paper:
 * "R.P. Salustowicz, J. Schmidhüber, Probabilistic incremental program evolution. Evol. Comput. 5(2), 123–141 (1997)".
 * In this tree, each node holds a probability vector that specifies the probability of a terminal/function appearing at that
 * location in a GP tree.
 */
public class PPTree extends Tree<PPTProbabilityVector>
{
    private PPTLearner learner;

    public PPTree(PPTProbabilityVector root)
    {
        super(root);
        if(root == null)
            throw new NullPointerException("Tree root cannot be null.");
    }


}

/**
 * An interface that defines the contract for learning strategies that PPT trees can use to learn from GP individuals.
 */
interface PPTLearner
{
    /**
     * Performs the learning mechanism that is a given PPT can use to learn from a given individual.
     * @param tree The PPT tree that is needed to be updated to learn from the given {@code individual}.
     * @param individual The individual that should be learned from.
     */
    void learnFrom(PPTree tree, GPIndividual individual);

    /**
     *
     */
    void initialize(PPTProbabilityVector tree);
}

class PIPELearner implements PPTLearner
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
     * Constructs a new instance.
     * @param PT The probability of selecting an instruction from the terminal set.
     * @param functions The name of all the GP functions.
     * @param terminals The name of all the GP terminals
     */
    public PIPELearner(EvolutionState state, int threadnum, double PT , String[] functions, String[] terminals)
    {
        this.state = state;
        this.threadnum = threadnum;
        this.PT = PT;
        this.functions = functions;
        this.terminals = terminals;
    }

    @Override
    public void learnFrom(PPTree tree, GPIndividual individual)
    {
        GPNode root = individual.trees[0].child;
    }

    @Override
    public void initialize(PPTProbabilityVector vector)
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

