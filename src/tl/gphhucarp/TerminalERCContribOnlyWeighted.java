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

public class TerminalERCContribOnlyWeighted extends TerminalERCWeighted
{
	private static final long serialVersionUID = 1L;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
		state.output.warning("TerminalERCContribOnlyWeighted loaded");
	}

	@Override
	double[] calculateWeights(List<GPNode> terminals)
	{
		weightUnnormalized = new double[terminals.size()];
		double maxFit = Double.MIN_VALUE; // Fitness is never negative so it's ok to use this MIN
		double minFit = Double.MAX_VALUE;
		for(GPNode terminal : terminals)
		{
			HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal.name());
			if(h == null)
			{
				System.err.println("Terminal " + terminal.name() + " not found in the book shelf.");
				continue;
			}
			for(GPIndividual ind : h.keySet())
			{
				double fitness = ind.fitness.fitness();
				if(fitness < minFit)
					minFit = fitness;
				if(fitness > maxFit)
					maxFit = fitness;
			}
		}

		double gmax = 1.0 / (1 + minFit);
		double gmin = 1.0 / (1 + maxFit);

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
				// int freq = stats.getFrequency();
				Pair<Double, Double> contrib = stats.getContribution();
				// int allIndTermSize = stats.getAllIndTerminals().size();
				double nfit = 1f / (ind.fitness.fitness() + 1);
				nfit = Math.max(0, (nfit - gmin)/(gmax - gmin));
				if(Math.abs(contrib.getValue() - contrib.getKey()) > 0.001)
				{
					weightUnnormalized[i] += nfit;
				}
				// System.out.println(Math.abs(contrib.getKey() - contrib.getValue()));
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
