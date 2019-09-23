package tl.knowledge.ppt.pipe;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.MersenneTwisterFast;
import tl.gp.GPIndividualUtils;
import tl.gphhucarp.UCARPUtils;

import java.io.Serializable;
import java.util.*;

/**
 * This class implements the Probabilistic Prototype Tree data structure introduced in the paper:
 * "R.P. Salustowicz, J. Schmidhüber, Probabilistic incremental program evolution. Evol. Comput. 5(2), 123–141 (1997)".
 * In this tree, each node holds a probability vector that specifies the probability of a terminal/function appearing at that
 * location in a GP tree.
 */
public class PPTree implements Serializable
{

	/**
	 * The learning algorithm that can learn the probability vector of this PPT.
	 */
    private IPIPELearner learner;

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
	public PPTree(IPIPELearner learner, String[] functions, String[] terminals)
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


//	public String sampleFrom(String address, MersenneTwisterFast twister)
//	{
//		if(address == null)
//			throw new NullPointerException("Node address cannot be null");
//		if(twister == null)
//			throw new NullPointerException("The random number generator cannot be null.");
//
//		ProbabilityVector node = this.nodes.get(address);
//		if(node == null)
//			return null;
//
//		return node.sample(twister);
//	}

	private static boolean isdigit(String str)
	{
		try
		{
			Double.parseDouble(str);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	/**
	 * Gets the probability of a GP item appearing at a given address. If the tree does not have a node at the given
	 * address, a new node will be created, initialized and added to the tree for that address.
	 * @param address the address of the node. This parameter cannot be {@code null} or empty.
	 * @param gpItem the GP terminal/function whose probability is wanted. This parameter cannot be {@code null} or
	 *               empty.
	 * @return the probability value of a node at the given address.
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
		return v.probabilityOf(gpItem);
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
			String nodeName = index.get(address).toString();
			if(isdigit(nodeName))
				nodeName = "ERC";
			double probability = getProbabilityOf(address, nodeName);
//			ProbabilityVector probabilityVector = nodes.get(address);
//			retval *= probabilityVector.probabilityOf(index.get(address).name());
			retval *= probability;
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

	/**
	 * Creates a new sample GP individual based on the probability tree that this object represents.
	 * @param twister a random number generator to initialise ERC nodes. This parameter cannot be {@code null}.
	 * @return the root node of a GP tree created based on this probability tree. The return value can be {@code null} if the
	 * tree is not initialised or is empty.
	 */
	public GPNode sampleIndividual(MersenneTwisterFast twister)
	{
		if(twister == null)
			throw new NullPointerException("Random generator cannot be null");

		String address = "-1";
		ProbabilityVector nodeProb = this.nodes.get(address);
		if(nodeProb == null)
			return null;
		String nodeName = nodeProb.sample(twister);
		if(nodeName == null)
			return null;

		GPNode root = UCARPUtils.createPrimitive(nodeName, twister.nextDouble());
		if(root.children.length > 0)
		{
			addChildren(root, "-1", twister);
		}

		return root;
	}

	private void addChildren(GPNode parent, String parentAddress, MersenneTwisterFast twister)
	{
//		if(parent.children.length <= 0)
//			return;
		for(int i = 0; i < parent.children.length; i++)
		{
			String childAdress = (parentAddress.equals("-1") ? "" : parentAddress) + i;
			ProbabilityVector nodeProb = this.nodes.get(childAdress);
			if(nodeProb == null)
				return;
			String nodeName = nodeProb.sample(twister);
			if(nodeName == null)
				return;

			parent.children[i] = UCARPUtils.createPrimitive(nodeName, twister.nextDouble());
			parent.children[i].parent = parent;
			parent.children[i].argposition = (byte) i;
			addChildren(parent.children[i], childAdress, twister);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder retval = new StringBuilder();
		List<String> sortedAdd = new ArrayList<>(nodes.keySet());
		Collections.sort(sortedAdd);
		for(String address : sortedAdd)
			retval.append("(").append(address).append(", ").append(nodes.get(address).toString()).append(")\n").append("\n");

		return retval.toString();
	}
}
