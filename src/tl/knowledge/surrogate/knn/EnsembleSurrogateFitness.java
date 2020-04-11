package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;

import java.util.*;
import java.util.function.Function;

public class EnsembleSurrogateFitness
{
	private final List<KNNSurrogateFitness> ensemble;
	private final double[] weights;
	private List<ReactiveDecisionSituation> situations;
	private List<HashMap<Individual, Double>> predictonMemory;

	public EnsembleSurrogateFitness(KNNSurrogateFitness... ensemble)
	{
		if(ensemble == null || ensemble.length == 0)
			throw new IllegalArgumentException("The ensemble of surrogates cannot be null or empty: "
					+ (ensemble == null));

		this.ensemble = Arrays.asList(ensemble);
		this.weights = new double[ensemble.length];
		predictonMemory = new ArrayList<>(weights.length);
		for (int i = 0; i < ensemble.length; i++)
		{
			predictonMemory.add(new HashMap<>());
			weights[i] = 1.0 / weights.length;
		}
	}

	public void updateWeights(Individual[] individuals)
	{
		double[] errors = new double[ensemble.size()];

		for (Individual individual : individuals)
		{
			double realFitness = individual.fitness.fitness();
			for (int surIndex = 0; surIndex < predictonMemory.size(); surIndex++)
			{
				HashMap<Individual, Double> surrogateMemory = predictonMemory.get(surIndex);
				double predicted = surrogateMemory.get(individual);
				errors[surIndex] = errors[surIndex] + Math.pow(predicted - realFitness, 2);
			}
		}
		predictonMemory.forEach(HashMap::clear); // There is no need for this memory once it has been used for updating the weights.

		double sumOfErros = 0;
		for (int i = 0; i < errors.length; i++)
		{
			errors[i] = Math.sqrt(errors[i] / individuals.length);
			sumOfErros += errors[i];
		}

		for (int i = 0; i < weights.length; i++)
		{
			if(sumOfErros == 0)
				weights[i] = 1.0 / weights.length;
			else
				weights[i] = (sumOfErros - errors[i]) / ((weights.length - 1)*sumOfErros);
		}
	}

	public double fitness(Individual ind)
	{
		double fitness = 0;
		for (int i = 0; i < ensemble.size(); i++)
		{
			KNNSurrogateFitness sur = ensemble.get(i);
			double surFit = sur.fitness(ind);
			predictonMemory.get(i).put(ind, surFit);
			fitness += weights[i] * surFit;
		}

		return fitness;
	}

	public void setFilter(PoolFilter filter)
	{
		for (KNNSurrogateFitness sur : ensemble)
		{
			sur.setFilter(filter);
		}
	}

	public boolean isKNNPoolEmpty()
	{
		for (KNNSurrogateFitness surr :	ensemble)
		{
			if (surr.isKNNPoolEmpty())
				return true;
		}

		return false;
	}


	public List<ReactiveDecisionSituation> getSituations()
	{
		return situations;
	}

	public void setSituations(List<ReactiveDecisionSituation> situations)
	{
		if(situations == null || situations.size() == 0)
			throw new RuntimeException("Situations cannot be null or empty");

		this.situations = situations;
		ensemble.forEach(sur->sur.setSituations(situations));
	}

	public List<GPIndividual> getSurrogatePool(int surrogateIndex)
	{
		if(surrogateIndex >= ensemble.size())
			throw new IllegalArgumentException("Surrogate index is a valid value: " + surrogateIndex
					+ ", ensemble size: " + ensemble.size());

		return ensemble.get(surrogateIndex).getSurrogatePool();
	}

	public void updateSurrogatePool(Individual[] population, String source)
	{
		ensemble.forEach(sur->sur.updateSurrogatePool(population, source));
	}

//	public String logSurrogatePool()
//	{
//		StringBuilder log = new StringBuilder();
//		for (int i = 0; i < ensemble.size(); i++) {
//			log.append("Sur ").append(i).append(":\n");
//			log.append(ensemble.get(i).logSurrogatePool()).append("\n\n");
//		}
//		return log.toString();
//	}

	public String logSurrogate()
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ensemble.size(); i++) {
			builder.append("Sur ").append(i).append(",");
			builder.append("weight ").append(i).append(",");
		}
		builder.append("ind\n");

		for(Individual ind: predictonMemory.get(0).keySet())
		{
			for(int i = 0; i < weights.length; i++)
			{
				double predictedFit = predictonMemory.get(i).get(ind);
				builder.append(predictedFit).append(",");
				builder.append(weights[i]).append(",");
			}
			builder.append(((GPIndividual) ind).trees[0].child.makeLispTree()).append("\n");
		}

		return builder.toString();
	}

	public void rerevaluate(Function<Individual, Double> with)
	{
		ensemble.forEach(sur -> sur.rerevaluate(with));
	}
}
