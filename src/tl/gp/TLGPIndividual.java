package tl.gp;

import ec.gp.GPIndividual;
import ec.gp.GPTree;

public class TLGPIndividual extends GPIndividual
{
	private static final long serialVersionUID = 1L;

	private boolean isTested = false;

	private double fitnessOnTrain;

//	@Override
//	public Object clone()
//	{
//		TLGPIndividual cl = (TLGPIndividual) super.clone();
//		cl.origin = "";
//		return cl;
//	}

	/**
	 * Gets the origin of this individual.
	 * @return The identity that created this individual.
	 */
	public String getOrigin()
	{
		return origin;
	}

	@Override
	public String toString()
	{
		StringBuilder retval = new StringBuilder();
		if(evaluated)
			retval.append("E,");
		else
			retval.append("Ne,");
		retval.append(fitness.fitness()).append(",");
		if(origin != null && !origin.isEmpty())
			retval.append(origin).append(",");
		for (int i = 0; i < trees.length; i++)
		{
			GPTree tree = trees[i];
			retval.append(tree.child.makeLispTree());
			if(i != trees.length - 1)
				retval.append(",");
		}

		return retval.toString();
	}

	/**
	 * Sets the origin of this individual.
	 * @param origin The identity that created this individual.
	 */
	public void setOrigin(String origin)
	{
		this.origin = origin;
	}

	/**
	 * Where was this individual originated from? What did create this individual?
	 */
	private String origin = "";

	public boolean isTested() {
		return isTested;
	}

	public void setTested(boolean tested)
	{
		isTested = tested;
	}

	public double getFitnessOnTrain() {
		return fitnessOnTrain;
	}

	public void setFitnessOnTrain(double fitnessOnTrain) {
		this.fitnessOnTrain = fitnessOnTrain;
	}
}

