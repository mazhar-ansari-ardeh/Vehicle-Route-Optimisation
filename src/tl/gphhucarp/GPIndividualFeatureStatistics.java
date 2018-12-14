package tl.gphhucarp;

import java.io.Serializable;
import java.util.ArrayList;

import ec.gp.GPIndividual;
import javafx.util.Pair;

/**
 *
 * @author mazhar
 */
public class GPIndividualFeatureStatistics implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int frequency = 0;

	private GPIndividual ind = null;

	private String terminal = null;

	private Pair<Double, Double> contribution;

	private ArrayList<String> allIndTerminals;

	public GPIndividualFeatureStatistics(GPIndividual gind, String terminal,
			ArrayList<String> allIndTermials, Pair<Double, Double> contrib)
	{
		this.contribution = contrib;
		this.terminal = terminal;
		this.ind = gind;
		this.allIndTerminals = allIndTermials;
	}

	public GPIndividual getIndividual()
	{
		return ind;
	}

	public ArrayList<String> getAllIndTerminals()
	{
		return allIndTerminals;
	}

	public String getTerminal()
	{
		return this.terminal;
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
