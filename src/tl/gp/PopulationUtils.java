package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import gphhucarp.decisionprocess.PoolFilter;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

	public static GPIndividual[] filterIndividuals(Individual[] pop, Predicate<GPIndividual> filter)
	{
		return Arrays.stream(pop).map(i -> (GPIndividual)i).filter(filter).toArray(GPIndividual[]::new);
	}

	public static void savePopulation(List<Individual> pop, String fileName)
			throws IOException
	{
		File file = new File(fileName);
		if(file.exists())
		{
			if(!file.delete())
				throw new RuntimeException("Failed to delete the existing file: " + file.getAbsolutePath());
		}
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
		{
			int nSubPops = 1;
			oos.writeInt(nSubPops);
			int nInds = pop.size();
			oos.writeInt(nInds);
			for(Individual ind : pop)
			{
				oos.writeObject(ind);
			}
		}
	}

	public static void savePopulation(Population pop, String fileName)
			throws IOException
	{
		File file = new File(fileName);
		if(file.exists())
		{
			if(!file.delete())
				throw new RuntimeException("Failed to delete the existing file: " + file.getAbsolutePath());
		}
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
			throws IOException, ClassNotFoundException
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
	 * Loads GP population from a file or a set of files in a directory. The method does not perform any clearing
	 * operation on the loaded pool to remove duplicated.
	 * @param state The evolutionary state that governs this process.
	 * @param inputPath The path to the knowledge file or directory.
	 * @param fromGeneration If the path is to a directory, the generation to start loading populations; otherwise, it
	 *                       is ignored.
	 * @param toGeneration If the path is to a directory, the generation to start loading populations; otherwise, it
	 *                     is ignored.
	 * @param logger The logger needed for logging the method. Error messages are always logged to this logger.
	 * @param logID The ID of the logger.
	 * @param logPopulation If {@code true}, the function will log all the loaded individuals at the end of its operation.
	 * @return A pool of individuals, sorted based on their fitness.
	 */
	public static List<Individual> loadPopulations(EvolutionState state, String inputPath, int fromGeneration,
												   int toGeneration, TLLogger<GPNode> logger, int logID,
												   boolean logPopulation) throws IOException, ClassNotFoundException
	{
		File f = new File(inputPath);

		List<Individual> pool = new ArrayList<>();
		if (f.isDirectory())
		{
			if (fromGeneration <= -1 || toGeneration <= -1)
			{
				if(logger != null)
					logger.log(state, logID, true, "Generation range is invalid: " + fromGeneration
							+ " to " + toGeneration + "\n");
				state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
						+ toGeneration);
			}
			for (int i = toGeneration; i >= fromGeneration; i--)
			{
				File file = Paths.get(inputPath, "population.gen." + i + ".bin").toFile();
				if(!file.exists())
				{
					if(logger != null)
						logger.log(state, logID, true, "The file " + file.toString() + " does not exist. Ignoring.\n");
					continue;
				}
				Population p = PopulationUtils.loadPopulation(file);
				pool.addAll(Arrays.asList(p.subpops[0].individuals));
//				pool.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
//				pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
			}
		}
		else if (f.isFile())
		{
			Population p = PopulationUtils.loadPopulation(f);
//			PopulationUtils.sort(p);
			pool.addAll(Arrays.asList(p.subpops[0].individuals));
//			pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
		}

		if(logPopulation && logger != null)
		{
			logger.log(state, logID, "pool size: " + pool.size() + "\n");
			pool.forEach(i ->
					logger.log(state, logID, false, i.fitness.fitness() + ", "
							+ ((GPIndividual) i).trees[0].child.makeLispTree() + "\n"));
		}
		return pool;
	}

	/**
	 * Loads GP population from a file or a set of files in a directory. The method performs a clearing operation on
	 * the loaded pool to remove duplicated.
	 * @param state The evolutionary state that governs this process.
	 * @param inputPath The path to the knowledge file or directory.
	 * @param fromGeneration If the path is to a directory, the generation to start loading populations; otherwise, it
	 *                       is ignored.
	 * @param toGeneration If the path is to a directory, the generation to start loading populations; otherwise, it
  	 *                     is ignored.
	 * @param metrics The similarity metric used for performing niching. This metric is expected to be configured before
	 *                passing it in.
	 * @param nicheRadius The niching radius. This parameter is used for population clearing.
	 * @param nicheCapacity The niching capacity. This parameter is used for population clearing.
	 * @param logger The logger needed for logging the method. Error messages are always logged to this logger.
	 * @param logID The ID of the logger.
	 * @param logPopulation If {@code true}, the function will log all the loaded individuals at the end of its operation.
	 * @return A pool of individuals, sorted based on their fitness.
	 */
	public static List<Individual> loadPopulations(EvolutionState state, String inputPath,
												   int fromGeneration, int toGeneration, PoolFilter filter,
												   SituationBasedTreeSimilarityMetric metrics,
												   double nicheRadius,
												   int nicheCapacity,
												   TLLogger<GPNode> logger,
												   int logID,
												   boolean logPopulation) throws IOException, ClassNotFoundException
	{
		File f = new File(inputPath);

		List<Individual> pool = new ArrayList<>();
		if (f.isDirectory())
		{
			if (fromGeneration <= -1 || toGeneration <= -1)
			{
				if(logger != null)
					logger.log(state, logID, true, "Generation range is invalid: " + fromGeneration
						+ " to " + toGeneration + "\n");
				state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
						+ toGeneration);
			}
			for (int i = toGeneration; i >= fromGeneration; i--)
			{
				File file = Paths.get(inputPath, "population.gen." + i + ".bin").toFile();
				if(!file.exists())
				{
					if(logger != null)
						logger.log(state, logID, true, "The file " + file.toString() + " does not exist. Ignoring.\n");
					continue;
				}
				Population p = PopulationUtils.loadPopulation(file);
				pool.addAll(Arrays.asList(p.subpops[0].individuals));
				pool.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
				SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, nicheRadius, nicheCapacity);
				pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
			}
		}
		else if (f.isFile())
		{
			Population p = PopulationUtils.loadPopulation(f);
			PopulationUtils.sort(p);
			pool.addAll(Arrays.asList(p.subpops[0].individuals));
			SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, nicheRadius, 1);
			pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
		}

		if(logPopulation && logger != null)
		{
			logger.log(state, logID, "pool size: " + pool.size() + "\n");
			for (int i1 = 0; i1 < pool.size(); i1++)
			{
				Individual i = pool.get(i1);
				logger.log(state, logID, false, i1 + ": " + i.fitness.fitness() + ", "
						+ ((GPIndividual) i).trees[0].child.makeLispTree() + "\n");
			}
		}
		return pool;
	}

	public static List<Individual> loadPopulations2(EvolutionState state, String inputPath,
												   int fromGeneration, int toGeneration, PoolFilter filter,
												   SituationBasedTreeSimilarityMetric metrics,
												   double nicheRadius,
												   int nicheCapacity,
												   TLLogger<GPNode> logger,
												   int logID,
												   boolean logPopulation) throws IOException, ClassNotFoundException
	{
		File f = new File(inputPath);

		List<Individual> pool = new ArrayList<>();
		if (f.isDirectory())
		{
			if (fromGeneration <= -1 || toGeneration <= -1)
			{
				if(logger != null)
					logger.log(state, logID, true, "Generation range is invalid: " + fromGeneration
							+ " to " + toGeneration + "\n");
				state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
						+ toGeneration);
			}
			for (int i = toGeneration; i >= fromGeneration; i--)
			{
				File file = Paths.get(inputPath, "population.gen." + i + ".bin").toFile();
				if(!file.exists())
				{
					if(logger != null)
						logger.log(state, logID, true, "The file " + file.toString() + " does not exist. Ignoring.\n");
					continue;
				}
				Population p = PopulationUtils.loadPopulation(file);
				List<Individual> loadedPop = Arrays.asList(p.subpops[0].individuals);
				loadedPop.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
				SimpleNichingAlgorithm.clearPopulation(loadedPop, filter, metrics, nicheRadius, nicheCapacity);
				loadedPop = loadedPop.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());

				pool.addAll(loadedPop);
//				pool.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
//				SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, nicheRadius, nicheCapacity);
//				pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
			}
		}
		else if (f.isFile())
		{
			Population p = PopulationUtils.loadPopulation(f);
			PopulationUtils.sort(p);
			pool.addAll(Arrays.asList(p.subpops[0].individuals));
			SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, nicheRadius, 1);
			pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());
		}

//		pool.sort(Comparator.comparingDouble(ind -> ind.fitness.fitness()));
//		SimpleNichingAlgorithm.clearPopulation(pool, filter, metrics, nicheRadius, nicheCapacity);
//		pool = pool.stream().filter(ind -> ind.fitness.fitness() != Double.POSITIVE_INFINITY).collect(Collectors.toList());

		if(logPopulation && logger != null)
		{
			logger.log(state, logID, "pool size: " + pool.size() + "\n");
			for (int i1 = 0; i1 < pool.size(); i1++)
			{
				Individual i = pool.get(i1);
				logger.log(state, logID, false, i1 + ": " + i.fitness.fitness() + ", "
						+ ((GPIndividual) i).trees[0].child.makeLispTree() + "\n");
			}
		}
		return pool;
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

	/**
	 * Performs a tournament selection on the given individuals.
	 * @param inds the set of individuals to select from. If this set is null or empty, -1 will be returned.
	 * @param state the {@code EvolutionState} object that is governing the evolutionary process.
	 * @param thread the number of the thread running the process.
	 * @param size the tournament size. This parameter cannot be zero or negative.
	 * @return the index of the selected individual.
	 */
	public static <T> int tournamentSelect(List<T> inds, Comparator<T> comparator, final EvolutionState state,
										   final int thread, int size)
	{
		if(inds == null || inds.size() == 0)
			return -1;

		if(size <= 0)
			throw new IllegalArgumentException("Tournament size needs to be a positive number: " + size);

		int best = state.random[thread].nextInt(inds.size());

		for (int x=1; x < size; x++)
		{
			int j = state.random[thread].nextInt(inds.size());
			if(comparator.compare(inds.get(j), inds.get(best)) < 0)
				best = j;
		}

		return best;
	}

	public static ArrayList<GPIndividual> rankSelect(GPIndividual[] pop, int sampleSize)
	{
		sort(pop);

		return new ArrayList<>(Arrays.asList(pop).subList(0, Math.min(pop.length, sampleSize)));
	}

	public static ArrayList<GPIndividual> rankSelect(List<GPIndividual> pop, int sampleSize)
	{
		if(sampleSize < 0)
			throw new IllegalArgumentException("Sample size cannot be a negative value: " + sampleSize);
		if(pop == null || pop.isEmpty())
			throw new IllegalArgumentException("Population cannot be null.");

		pop.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));

		return new ArrayList<>(pop.subList(0, Math.min(pop.size(), sampleSize)));
	}

	public static Population loadPopulation(String fileName)
			throws IOException, ClassNotFoundException
	{
		File file = new File(fileName);
		return loadPopulation(file);
	}

//	public static <E> ArrayList<List<E>> partition(Collection<E> pool, BiFunction<E, E, Double> dist)
//	{ // this code is buggy.
//		Queue<E> cPool = new ConcurrentLinkedQueue<>(pool);
//
//		ArrayList<List<E>> partitions = new ArrayList<>();
//
//		for (E p : cPool)
//		{
//			List<E> part = cPool.stream().filter(in -> dist.apply(in, p) <= 1).collect(Collectors.toList());
//			cPool.removeAll(part);
//			if(!part.isEmpty())
//				partitions.add(part);
//		}
//
//		return partitions;
//	}
}
