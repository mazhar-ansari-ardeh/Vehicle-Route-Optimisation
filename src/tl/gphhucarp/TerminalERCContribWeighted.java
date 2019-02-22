package tl.gphhucarp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import ec.util.RandomChoice;
import javafx.util.Pair;

public class TerminalERCContribWeighted extends TerminalERCWeighted
{
	private static final long serialVersionUID = 1L;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
		state.output.warning("TerminalERCContribWeighted loaded");
	}

	@Override
	double[] calculateWeights(EvolutionState state, List<GPNode> terminals)
	{
		weightUnnormalized = new double[terminals.size()];

		for (int i = 0; i < terminals.size(); i++)
		{
			//	System.out.println(terminals.get(i) + ", " + terminals.get(i).hashCode());
			//	book.keySet().forEach(node -> System.out.println(node + ", " + node.hashCode()));
			GPNode terminal = terminals.get(i);
			HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal.name());
			if(h == null)
			{
				System.err.println("Terminal " + terminal.name() + " not found in the book shelf.");
				continue;
			}
			for(GPIndividual ind: h.keySet())
			{
				GPIndividualFeatureStatistics stats = h.get(ind);
				int freq = stats.getFrequency();
				Pair<Double, Double> contrib = stats.getContribution();
				int allIndTermSize = stats.getAllIndTerminals().size();
				double nfit = 1f / (ind.fitness.fitness() + 1);
				// System.out.println(Math.abs(contrib.getKey() - contrib.getValue()));
				weightUnnormalized[i] += (
							   Math.abs(contrib.getKey() - contrib.getValue())
							   * nfit
							   * freq
							   ) / allIndTermSize;
			}

			// weights[i] = useCount;
		}
		// weights = Arrays.copyOf(weights, weights.length);
		System.out.println("Terminal weights: ");
		for(int i = 0; i < terminals.size(); i++)
		{
			System.out.println(terminals.get(i) + ": " + weightUnnormalized[i]);
		}
		weights = Arrays.copyOf(weightUnnormalized, weightUnnormalized.length);
		RandomChoice.organizeDistribution(weights, true);

		return weights;
	}
}
