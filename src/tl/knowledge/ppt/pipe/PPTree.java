package tl.knowledge.ppt.pipe;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.gp.GPIndividualUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the Probabilistic Prototype Tree data structure introduced in the paper:
 * "R.P. Salustowicz, J. Schmidhüber, Probabilistic incremental program evolution. Evol. Comput. 5(2), 123–141 (1997)".
 * In this tree, each node holds a probability vector that specifies the probability of a terminal/function appearing at that
 * location in a GP tree.
 */
class PPTree
{

	/**
	 * The learning algorithm that can learn the probability vector of this PPT.
	 */
    private PIPELearner learner;

	/**
	 * The set of GP functions.
	 */
	private String[] functions;

	/**
	 * The set of GP terminals.
	 */
    private String[] terminals;

	/**
	 * Stores the probability vector for each node of a PIPE PPT. This field maps a string that represents the
	 * address of a node to a probability vector of that node. The used address scheme is: <br/>
	 * <p> - The address of the root node is "-1",
	 * <p> - The address of each node is the address of its parent that is appended with the position of the node
	 * amongst its parent's children.
	 *
	 * Technically, it is possible to use other schemes.
	 */
	private HashMap<String, ProbabilityVector> nodes = new HashMap<>();

	/**
	 * Creates a new instance of this class.
	 * @param learner The learner for this object.
	 * @param functions the set of GP functions. This parameter cannot be {@code null}. However, it is possible to have
	 *                  a set of length zero.
	 * @param terminals the set of GP terminals. This parameter cannot be {@code null}. However, it is possible to have
	 *                  a set of length zero.
	 */
	public PPTree(PIPELearner learner, String[] functions, String[] terminals)
	{
		if(learner == null)
			throw new IllegalArgumentException("Learner cannot be null.");
		if(terminals == null || functions == null)
			throw new IllegalArgumentException("Function set or terminal set cannot be null.");

		this.functions = functions;
		this.terminals = terminals;

		this.learner = learner;

		// According to the paper: "Initially, the PPT contains the root node."
		ProbabilityVector rootProb = new ProbabilityVector(terminals, functions);
		learner.initialize(rootProb);
		nodes.put("-1", rootProb);
	}

	/**
	 * Gets the probability vector at a given address. If the tree does not have a node at the given address, a new node
	 * will be created, initialized and added to the tree for that address.
	 * @param address the address of the node. This parameter cannot be {@code null} or empty.
	 * @return the probability vector at the given address.
	 */
	ProbabilityVector getProbabilityOf(String address)
	{
		if(address == null || address.isEmpty())
			throw new IllegalArgumentException("Node address cannot be null or empty");

		if(nodes.containsKey(address))
			return nodes.get(address);

		// According to the paper, nodes are created on demand whenever I(d,w)\in F is selected and the subtree for an
		// argument of I(d, w) is missing.
		ProbabilityVector nodeProb = new ProbabilityVector(this.terminals, this.functions);
		learner.initialize(nodeProb);
		nodes.put(address, nodeProb);
		return nodeProb;
	}

	/**
	 * Gets the probability of a GP item appearing at a given address. If the tree does not have a node at the given
	 * address, a new node will be created, initialized and added to the tree for that address.
	 * @param address the address of the node. This parameter cannot be {@code null} or empty.
	 * @param gpItem the GP terminal/function whose probability is wanted. This parameter cannot be {@code null} or
	 *               empty.
	 * @return the probability vector at the given address.
	 */
	public double getProbabilityOf(String address, String gpItem)
	{
		if(address == null || address.isEmpty())
			throw new IllegalArgumentException("Node address cannot be null or empty");
		if(gpItem == null || gpItem.isEmpty())
			throw new IllegalArgumentException("GP terminal/function name cannot be null or empty");

		if(!nodes.containsKey(address))
		{
			// According to the paper, nodes are created on demand whenever I(d,w)\in F is selected and the subtree for an
			// argument of I(d, w) is missing.
			ProbabilityVector nodeProb = new ProbabilityVector(this.terminals, this.functions);
			learner.initialize(nodeProb);
			nodes.put(address, nodeProb);
		}

		ProbabilityVector v = nodes.get(address);
		double nodeProb = v.probabilityOf(gpItem);
		return nodeProb;
	}

	/**
	 * Sets the probability value of the given GP item at the given node address.
	 * @param address the address of the node to be updated. This parameter cannot be {@code null}.
	 * @param gpItem the GP function/terminal whose probability value is going to be updated. This parameter cannot be
	 * 				 {@code null}.
	 * @param newProbability the new probability value to set.
	 */
	public void setProbabilityOf(String address, String gpItem, double newProbability)
	{
		if(address == null || address.isEmpty())
			throw new IllegalArgumentException("Node address cannot be null or empty");
		if(gpItem == null || gpItem.isEmpty())
			throw new IllegalArgumentException("GP terminal/function name cannot be null or empty");
		if(newProbability < 0 || newProbability > 1)
			throw new IllegalArgumentException("Probability value should be in the range [0, 1]:" + newProbability);

		// TODO: 28/08/19 Is this a good idea?
		if(!nodes.containsKey(address))
		{
			// According to the paper, nodes are created on demand whenever I(d,w)\in F is selected and the subtree for an
			// argument of I(d, w) is missing.
			ProbabilityVector nodeProb = new ProbabilityVector(this.terminals, this.functions);
			learner.initialize(nodeProb);
			nodes.put(address, nodeProb);
		}
		ProbabilityVector v = nodes.get(address);
		v.setProbabilityOf(gpItem, newProbability);
	}

	/**
	 * Calculates the probability of an individual to happen based on this PPT.
	 * @param ind an individual which whose probability of being created with this PPT is desired.
	 * @param tree the index of the tree in the given GP individual to consider.
	 * @return the probability that the individual may be created with this PPT.
	 */
	public double probabilityOf(GPIndividual ind, int tree)
	{
		if(ind == null || ind.trees == null || ind.trees.length == 0)
			throw new IllegalArgumentException("The given GP individual is null or does not contain any GP trees.");
		if(tree < 0)
			throw new IllegalArgumentException("Tree index is invalid: " + tree);

		double retval = 1;
		Map<String, GPNode> index = GPIndividualUtils.index(ind.trees[tree].child);
		for(String address : index.keySet())
		{
			ProbabilityVector probabilityVector = nodes.get(address);
			retval *= probabilityVector.probabilityOf(index.get(address).name());
		}
		return retval;
	}

	/**
	 * Sets the value of the random constant of the node pointed to by the {@code address} to the value given.
	 * @param address the address of the node to be updated. This parameter cannot be {@code null}.
	 * @param value the new value of the random constant
	 */
	public void setR(String address, Double value)
	{
		if(address == null || address.isEmpty())
			throw new IllegalArgumentException("Node address cannot be null or empty");

		nodes.get(address).setR(value);
	}
}
