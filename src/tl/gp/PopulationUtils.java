package tl.gp;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

import ec.*;

public class PopulationWriter implements AutoCloseable
{
	private boolean isSaving;
	private File file;
	ObjectOutputStream output = null;

	public PopulationWriter(String pathname, boolean save)
	{
		this.isSaving = save;
		file = new File(pathname);
	}

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

	public void prepareForWriting(Population population, Subpopulation sub) throws IOException
	{
		if(!isSaving)
			throw new IOException("This object is not initialized for saving objects.");
		if(output == null)
		{
			output = new ObjectOutputStream(new FileOutputStream(file));
			output.writeInt(population.subpops.length);
		}

		output.writeInt(sub.individuals.length);
	}

	public void write(Individual ind) throws IOException
	{
		if(!isSaving)
			throw new IOException("This object is not initialized for saving objects.");

		output.writeObject(ind);
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

	public static Population loadPopulation(String fileName)
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		File file = new File(fileName);
		return loadPopulation(file);
	}

	@Override
	public void close() throws Exception
	{
		if(output != null)
		{
			output.flush();
			output.close();
			output = null;
			file = null;
		}
	}
}
