package tl.gphhucarp;

import java.io.Serializable;

import javafx.util.Pair;

/**
 *
 * @author mazhar
 */
public class GPIndividualFeatureStatistics implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int frequency = 0;

	private Pair<Double, Double> contribution;

	public GPIndividualFeatureStatistics(Pair<Double, Double> contrib)
	{
		this.contribution = contrib;
	}

	public int getFrequency()
	{
		return frequency;
	}

	public void incrementFrequency()
	{
		++frequency;
	}

	public void setContribution(Pair<Double, Double> contrib)
	{
		contribution = contrib;
	}

	public Pair<Double, Double> getContribution()
	{
		return contribution;
	}

	public String toString()
	{
		return "freg: " + frequency + ", contrib: (" + contribution.getKey() + ", "
						+ contribution.getValue() + ")";
	}
}
