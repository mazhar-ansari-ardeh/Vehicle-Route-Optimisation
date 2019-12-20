package tl.knowledge.ppt.pipe;

import ec.EvolutionState;
import ec.util.MersenneTwisterFast;
import ec.util.RandomChoice;

import java.io.Serializable;
import java.util.HashMap;

class RouletteWheelSelector
{
//	int[] length;
	double[] probability;
	int currentIndex = 0;
//	int maxLength = 0;

//	RouletteWheelSelector(int size)
//	{
//		probability =new double[size];
//	}

	public RouletteWheelSelector(double[] weights)
	{
		probability = new double[weights.length];

		double sum = 0;
		for(double d : weights)
			sum += d;

		for(int i = 0; i < weights.length; i++)
		{
			add(weights[i] / sum);
		}
	}

	void add(double currentProbability)
	{
//		length[currentIndex] = currentLength;
		probability[currentIndex] = currentProbability;
		currentIndex = currentIndex +1;
//		if(currentLength > maxLength) maxLength = currentLength;
	}

	public int roulette(MersenneTwisterFast twisterFast)
	{
		int winner = 0;
//		int selectedLength = 0;
		// accumulate
		for (int i = 1; i < currentIndex; i++)
		{
			probability[i] += probability[i-1];
		}

		int bot = 0; // binary chop search
		int top = currentIndex - 1;
		double f = twisterFast.nextDouble() * probability[top];

		for(int loop =0; loop< 20; loop++)
		{
			int index = (top + bot) / 2;
			if (index > 0 && f < probability[index - 1])
				top = index - 1;
			else if (f > probability[index])
				bot = index + 1;
			else
			{
				if (f == probability[index] && index + 1 < currentIndex)
					winner = index + 1;
				else
					winner = index;
				break;
			}
		}
		// check for bounds
		if(winner < 0 || winner >= currentIndex)
		{
//			state.output.fatal("roulette() method  winner " + winner + " out of range 0..." + (currentIndex-1));
			winner=0; //safe default
		}
//		if(length[winner] < 1 || length[winner] > maxLength)
//		{
//			state.output.fatal("roulette() method " + length[winner] + " is  out of range 1..." + maxLength);
//			// range is specified on creation
//			return maxLength; //safe default
//		}
//		selectedLength = length[winner];
		return winner;
	}

}


class ProbabilityVector implements Serializable
{
	private final static long serialVersionUID = -8435141646461582571L;

	/**
	 * Get the minimum probability threshold.
	 * @return the minimum probability threshold
	 */
	public double getMinThreshold()
	{
		return minThreshold;
	}

	/**
     * The minimum weight that each terminal/function can have. If the weight of a terminal/function is less than this
     * value, this value will be used instead. This threshold is a soft threshold, meaning that the actual weight of
     * terminals/functions are not modified but instead, whenever the weight is used and its value is less than the threshold,
     * threshold is used.
     */
	private double minThreshold;

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

	/**
	 * If {@code true}, this object is initialized.
	 */
	private boolean initialized = false;

	/**
	 * Creates a new instance of the {@code ProbabilityVector} with the given set of terminals, functions and minimum
	 * probability threshold.
	 * @param terminals The set of GP terminals that this object should hold probabilities for.
	 * @param functions The set of GP functions that this object should hold probabilities for.
	 * @param threshold The minimum probability threshold. If the probability of an item is less than this threshold, the
	 *                  threshold value will be used.
	 */
	public ProbabilityVector(String[] terminals, String[] functions, double threshold)
	{
		if(terminals == null || functions == null)
			throw new IllegalArgumentException("Terminal set or function set is null");

		if(threshold < 0)
		    throw new IllegalArgumentException("Threshold cannot be a negative value");

		for(String terminal : terminals)
			probabilities.put(terminal, 0d);
		for(String function : functions)
			probabilities.put(function, 0d);

		this.minThreshold = threshold;
	}

//	/**
//	 * Creates a new instance of the {@code ProbabilityVector} with the given set of terminals, functions and minimum
//	 * probability threshold. The minimum probability threshold will be set to zero.
//	 * @param terminals The set of GP terminals that this object should hold probabilities for.
//	 * @param functions The set of GP functions that this object should hold probabilities for.
//	 */
//    public ProbabilityVector(String[] terminals, String[] functions)
//    {
//        this(terminals, functions, 0);
//    }



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
	 * Randomly selects a primitive type from the vector of the primitives that this object contains with a roulette wheel
	 * selection mechanism.
	 * @param twisterFast the random number generator to use for selecting a primitive item. This parameter cannot be
	 * {@code null}.
	 * @return the name of the selected item. The returned value will be {@code null} if this probability vector is empty.
	 */
	public String sample(MersenneTwisterFast twisterFast)
	{
		if(twisterFast == null)
			throw new NullPointerException("The random number generator cannot be null.");

		if(probabilities.size() == 0)
			return null;

		// This is an inefficient way of doing this but I am in a hurry at the moment.
		double[] probs = new double[probabilities.size()];
		String[] names = new String[probabilities.size()];
		int i = 0;
		for(String name : probabilities.keySet())
		{
			names[i] = name;
			probs[i] = probabilities.get(name);
			if(probs[i] < minThreshold)
				probs[i] = minThreshold;
			i++;
		}

//		RouletteWheelSelector rw = new RouletteWheelSelector(probs);
//		int winner = rw.roulette(twisterFast);
		RandomChoice.organizeDistribution(probs);
		int winner = RandomChoice.pickFromDistribution(probs, twisterFast.nextDouble());
		return names[winner];
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

	@Override
	public String toString()
	{
		StringBuilder bl = new StringBuilder();
		bl.append("[");
		for(String s : this.probabilities.keySet())
			bl.append("(").append(s).append(", ").append(probabilities.get(s)).append(")").append(", ");

		bl.append("minThresh: ").append(minThreshold).append(", ").append("R: ").append(R).append("]");

		return bl.toString();
	}

	/**
	 * Gets a simplified version of the string representation of this vector. The simplified version does not contain the
	 * probabilities that are zero.
	 * @return Simplified string representation of this probability vector.
	 */
	public String toSimplifiedString()
	{
		StringBuilder bl = new StringBuilder();
		bl.append("{");
		for(String s : this.probabilities.keySet())
		{
			if(probabilities.get(s) > 0)
			{
				bl.append(s).append(": ").append(Math.round(probabilities.get(s) * 100) / 100.0).append("|");
			}
		}
		if(bl.toString().endsWith("|"))
			bl.deleteCharAt(bl.length() -1);
		if(minThreshold > 0)
			bl.append("|miTh:").append(minThreshold);
		bl.append("}");
//		bl.append(R).append("}");

		return bl.toString();
	}
}