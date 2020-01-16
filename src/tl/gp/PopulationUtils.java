package tl.gp;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

import ec.*;
import ec.gp.GPIndividual;

public class PopulationUtils
{
	/**
	 * Sorts individuals based on their fitness. The method iterates over all subpopulations
	 * and sorts the array of individuals in them based on their fitness so that the first
	 * individual has the best (i.e. least) fitness.
	 *
	 * @param pop a population to sort. Can't be {@code null}.
	 * @return the given pop with individuals sorted.
	 */
	public static Population sort(Population pop)
	{
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		for(Subpopulation subpop : pop.subpops)
			Arrays.sort(subpop.individuals, comp);

		return pop;
	}

	public static void sort(Individual[] ind)
	{
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(ind, comp);
	}

	public static void sort(GPIndividual[] ind)
	{
		Comparator<GPIndividual> comp = (GPIndividual o1, GPIndividual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(ind, comp);
	}

	public static GPIndividual[] filterIndividuals(GPIndividual[] pop, Predicate<GPIndividual> filter)
	{
		return Arrays.stream(pop).filter(filter).toArray(GPIndividual[]::new);
	}

	public static void savePopulation(Population pop, String fileName)
			throws FileNotFoundException, IOException
	{
		File file = new File(fileName);
		if(file.exists())
			file.delete();
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
		{
			int nSubPops = pop.subpops.length;
			oos.writeInt(nSubPops);
			for(Subpopulation subpop : pop.subpops)
			{
				int nInds = subpop.individuals.length;
				oos.writeInt(nInds);
				for(Individual ind : subpop.individuals)
				{
					oos.writeObject(ind);
				}
			}
		}
	}

	public static Population loadPopulation(File file)
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		Population retval = new Population();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)))
		{
			int numSub = ois.readInt();
			retval.subpops = new Subpopulation[numSub];
			for(int subInd = 0; subInd < numSub; subInd++)
			{
				int numInd = ois.readInt();
				retval.subpops[subInd] = new Subpopulation();
				retval.subpops[subInd].individuals = new Individual[numInd];
				for(int indIndex = 0; indIndex < numInd; indIndex++)
				{
					Object ind = ois.readObject();
					if(!(ind instanceof Individual))
						throw new InvalidObjectException("The file contains an object that is not "
								+ "instance of Individual: " + ind.getClass().toString());
					retval.subpops[subInd].individuals[indIndex] = (Individual)ind;
				}
			}
		}

		return retval;
	}


	/**
	 * Performs a tournament selection on the given individuals.
	 * @param inds the set of individuals to select from. This set cannot be {@code null}.
	 * @param state the {@code EvolutionState} object that is governing the evolutionary process.
	 * @param thread the number of the thread running the process.
	 * @param size the tournament size. This parameter cannot be zero or negative.
	 * @return the index of the selected individual.
	 */
	public static int tournamentSelect(Individual[] inds, final EvolutionState state, final int thread, int size)
	{
		if(inds == null || inds.length == 0)
			throw new IllegalArgumentException("The individual set cannot be null or empty.");

		if(size <= 0)
			throw new IllegalArgumentException("Tournament size needs to be a positive number: " + size);

		int best = state.random[thread].nextInt(inds.length);

		for (int x=1; x < size; x++)
		{
			int j = state.random[thread].nextInt(inds.length);
			if (inds[j].fitness.betterThan(inds[best].fitness))  // j is better than best
				best = j;
		}

		return best;
	}

	public static ArrayList<GPIndividual> rankSelect(GPIndividual[] pop, int sampleSize)
	{
		sort(pop);

		return new ArrayList<>(Arrays.asList(pop).subList(0, pop.length >= sampleSize ? sampleSize : pop.length));
	}

	public static Population loadPopulation(String fileName)
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		File file = new File(fileName);
		return loadPopulation(file);
	}
}
