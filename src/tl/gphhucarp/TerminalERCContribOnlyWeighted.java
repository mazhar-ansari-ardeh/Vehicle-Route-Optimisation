package tl.gphhucarp;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;

import ec.EvolutionState;
import ec.gp.ERC;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import ec.util.RandomChoice;
import javafx.util.Pair;

public class TerminalERCContribOnlyWeighted extends TerminalERCWeighted
{
	private static final long serialVersionUID = 1L;

	/**
	 * The parameter to define the minimum weight for a feature. If the normalized weight of a
	 * feature is less than this value, the value specified for this parameter will be used instead.
	 * The value for this parameter needs to be in [0, 1]. The value of this parameter is used only
	 * when weight normalization is activated.
	 */
	public static final String P_MIN_WEIGHT = "min-weight";

	/**
	 * The parameter to define the minimum weight for a feature. If the normalized weight of a
	 * feature is less than this value, the value specified for this parameter will be used instead.
	 * The value for this parameter needs to be in [0, 1]. The value of this parameter is used only
	 * when weight normalization is activated.
	 */
	public static final String P_MAX_WEIGHT = "max-weight";

	/**
	 * A boolean parameter that if is {@code true}, weight of ERC terminal will not be calculated
	 * based on its contribution in the source domain and instead, is calculated uniformly as (1/N)
	 * in which N is the number of terminals. If the calculated uniform weight is greater not within
	 * thresholds, it will be shifted.
	 */
	public static final String P_UNIFORM_ERC = "uniform-erc";
	private boolean uniformERC;

	/**
	 * If {@code true}, weights of terminals will be normalized before use.
	 */
	public static final String P_NORMALIZE_WEIGHTS = "normalize-weights";
	private boolean normalizeWeights;

	private double minWeight;
	private double maxWeight;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter p = base.push(P_NORMALIZE_WEIGHTS);
		normalizeWeights = state.parameters.getBoolean(p, null, true);
		if(!normalizeWeights)
		{
			state.output.warning("TerminalERCContribOnlyWeighted loaded with normalization "
								+ "disabled.");
			return;
		}

		p = base.push(P_UNIFORM_ERC);
		uniformERC = state.parameters.getBoolean(p, null, false);

		p = base.push(P_MAX_WEIGHT);
		maxWeight = state.parameters.getDouble(p, null);
		if(maxWeight < 0 || maxWeight > 1)
			state.output.fatal("Invalid max weight: " + maxWeight);

		p = base.push(P_MIN_WEIGHT);
		minWeight = state.parameters.getDouble(p, null);
		if(minWeight < 0 || minWeight > 1)
			state.output.fatal("Invalid min weight: " + minWeight);

		state.output.warning("TerminalERCContribOnlyWeighted loaded. Min weight: " + minWeight
				+ ", max weight: " + maxWeight + ", uniform-erc: " + uniformERC);
	}

	@Override
	double[] calculateWeights(EvolutionState state, List<GPNode> terminals)
	{
		weightUnnormalized = new double[terminals.size()];

		double gmax = 1.0 / (1 + minFit);
		double gmin = 1.0 / (1 + maxFit);

		for (int i = 0; i < terminals.size(); i++)
		{
			GPNode terminal = terminals.get(i);
			HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal.name());
			if(h == null)
			{
				state.output.warning("Terminal " + terminal.name()
									 			 + " not found in the book shelf.\n");
				continue;
			}
			for(GPIndividual ind: h.keySet())
			{
				GPIndividualFeatureStatistics stats = h.get(ind);
				Pair<Double, Double> contrib = stats.getContribution();
				double nfit = 1f / (ind.fitness.fitness() + 1);
				nfit = Math.max(0, (nfit - gmin)/(gmax - gmin));
				if(Math.abs(contrib.getValue() - contrib.getKey()) > 0.001)
				{
					weightUnnormalized[i] += nfit;
				}
			}
		}
		double[] normalizedWeights = Arrays.copyOf(weightUnnormalized, weightUnnormalized.length);

		if(normalizeWeights)
		{
			normalizedWeights = normalizeWeights(state, terminals);
		}
		else
		{
			for(int i = 0; i < terminals.size(); i++)
			{
				log(state, logID, terminals.get(i) + ": "+ weightUnnormalized[i] + ", normalized: "
						   + normalizedWeights[i] + "\n");
			}
		}
		weights = Arrays.copyOf(normalizedWeights, normalizedWeights.length);
		RandomChoice.organizeDistribution(weights, true);

		return weights;
	}

	private double[] normalizeWeights(EvolutionState state, List<GPNode> terminals)
	{
		DoubleSummaryStatistics stat = Arrays.stream(weightUnnormalized).summaryStatistics();
		double sum = stat.getSum();
		if(sum == 0)
		{
			state.output.warning("Sum of feature weights is zero.");
			log(state, logID, "Sum of feature weights is zero. Results will not be reliable.\n");
		}
		double[] normalizedWeights = Arrays.copyOf(weightUnnormalized, weightUnnormalized.length);

		log(state, logID, "Terminal weights: \n");
		for(int i = 0; i < terminals.size(); i++)
		{
			if(terminals.get(i) instanceof ERC && uniformERC)
			{
				normalizedWeights[i] = 1f / terminals.size();
				log(state, logID, "ERC weighted uniformly. Original weight: "
								   + (weightUnnormalized[i]) / (sum) + ", uniform weight: "
								   + normalizedWeights[i] + "\n");
			}
			else
				normalizedWeights[i] = (weightUnnormalized[i]) / (sum);

			if(normalizedWeights[i] < minWeight)
			{
				log(state, logID, terminals.get(i) + ": " + weightUnnormalized[i] + ", normalized: "
						   + normalizedWeights[i] + " is lower than threshold. Threshold used.\n");
				normalizedWeights[i] = minWeight;
			}
			if(normalizedWeights[i] > maxWeight)
			{
				log(state, logID, terminals.get(i) + ": " + weightUnnormalized[i] + ", normalized: "
						   + normalizedWeights[i]
						   + " is greater than threshold. Threshold used.\n");
				normalizedWeights[i] = maxWeight;
			}

			log(state, logID, terminals.get(i) + ": " + weightUnnormalized[i] + ", normalized: "
											   + normalizedWeights[i] + "\n");
		}

		return normalizedWeights;
	}
}
