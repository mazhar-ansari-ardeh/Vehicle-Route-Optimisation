package tl.knowledge.ppt.pipe;

import java.util.HashMap;

class ProbabilityVector
{
	/**
	 * The structure that maps terminal/function name to its probability.
	 */
	HashMap<String, Double> probabilities = new HashMap<>();

	/**
	 * R is a generic random constant. In the paper, it is in the range [0, 1)
	 * When this probability vector is used to sample a node, if ERC terminal is selected, then the value of the node
	 * will be generated randomly, unless the probability of ERC is greater that a threshold TR. In this case, the value
	 * in this field will be used.
	 */
	double R;

	/*
	 * List of all the terminal names (including ERC) that can appear in GP system.
	 */
//	private String[] terminals;

	/*
	 * List of all the function names that can appear in GP system.
	 */
//	private String[] functions;

	/**
	 * If {@code true}, this object is initialized.
	 */
	private boolean initialized = false;

	public ProbabilityVector(String[] terminals, String[] functions)
	{
		if(terminals == null || functions == null)
			throw new IllegalArgumentException("Terminal set or function set is null");

//		this.terminals = terminals;
//		this.functions = functions;
		for(String terminal : terminals)
			probabilities.put(terminal, 0d);
		for(String function : functions)
			probabilities.put(function, 0d);
	}

	/**
	 * Checks if the PPTree is initialized for learning or not. Being initialized is the first step towards learning a
	 * PPT.
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

	/**
	 * Returns the probability value of an item in the probability vector. The method does not check if the vector is
	 * initialized or not.
	 * @param item an item. This parameter cannot be null or empty.
	 * @return the probability value of the given item.
	 * @throws  RuntimeException if the given item is not inside the array.
	 */
	public double probabilityOf(String item)
	{
		if(item == null || item.isEmpty())
			throw new IllegalArgumentException("The given item cannot be null or empty");
		if (!probabilities.containsKey(item))
			throw new RuntimeException("The given item: " + item + " is not inside the array.");

		return probabilities.get(item);
	}

	/**
	 * Sets the probability value of the given GP item.
	 * @param item the GP function/terminal whose probability value is going to be updated.
	 * @param value the new probability value to set.
	 * @throws RuntimeException if the given item is not inside the array.
	 */
	public void setProbabilityOf(String item, double value)
	{
		if(item == null || item.isEmpty())
			throw new IllegalArgumentException("The given item cannot be null or empty");
		if (!probabilities.containsKey(item))
			throw new RuntimeException("The given item: " + item + " is not inside the array.");
		if(value < 0 || value > 1)
			throw new RuntimeException("The probability value cannot be less than zero or greater than 1:" + value);

		probabilities.put(item, value);
	}

	/**
	 * Set the value of the random constant
	 * @param R the new value for the random constant
	 */
	public void setR(double R)
	{
		this.R = R;
	}

	/**
	 * Gets the value of the random constant.
	 */
	public double getR()
	{
		return this.R;
	}
}