package tl.gp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleStatistics;
import ec.util.Parameter;
import gphhucarp.gp.ReactiveGPHHProblem;


/**
 * This class implements a customization of the <code>SimpleStatistics</code> class. This class has
 * added the following modifications to its base class: <p>
 * * Modified the <code>postInitializationStatistics</code> hook so that it will write a copy of
 * the initial population, with fitness of each individual, to a file. The name of the file is read
 * from a new parameter: <code>P_INIT_POP_FILE_NAME</code> and if it is not present, a default value
 * of <code>"init-pop.dat"</code> will be used.
 * @author mazhar
 *
 */
public class FCFStatistics extends SimpleStatistics
{
	private static final long serialVersionUID = 1L;

	/**
	 * The ID of the logger that will be used to log statistics of each generation of GP.
	 */
	private int statLogID;


	// private int knowledgeLogID;

	/**
	 * The name that will be used to save population in generation. This is the base name and it
	 * will be augmented for each generation.
	 */
	private String genPopFileName = "";

	/**
	 * If <code>true</code>, population of GP will be saved after each generation.
	 */
	private boolean saveGenerations = false;


	public final static String P_GEN_POP_FILE_NAME = "gen-pop-file";
	public final static String P_SAVE_POP = "save-pop";
	// public final static String P_SAVE_POP_TREE = "save-tree";

	/**
	 * The best generation, i.e. the generation that has the individual with the best fitness.
	 */
	int bestGeneration = 0;
	double bestGenerationFitness = Double.MAX_VALUE;

	/**
	 * Iterate population and perform a collection of tasks defined inside the function.
	 * @param state The state of the evolution.
	 */
	private void iteratePopulation(EvolutionState state)
	{
		try
		{
			// 1. initializing iteration operations

			// 1.1 Prepare for saving population.
			String popFileName = generatePopulationFileName(state.generation);
			File f = new File(popFileName);
			if(f.exists())
				f.delete();
			f.createNewFile();

			ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(f));
			writer.writeInt(state.population.subpops.length);

			for(Subpopulation sub: state.population.subpops)
			{
				// 2. initialization for subpopulation iteration

				// 2.1 prepare for saving subpopulation
				writer.writeInt(sub.individuals.length);

				// 2.2 prepare for subpopulation statistics
				double bestSubPopFitness = Double.MAX_VALUE;
				double fitnessSum = 0;


				for(Individual ind: sub.individuals)
				{
					// 3. Do the actual work

					// 3.1 save individuals
					writer.writeObject(ind);

					// 3.2 Gather statistics
					GPIndividual gind = (GPIndividual)ind;
					Fitness fit = gind.fitness;
					double fitness = fit.fitness();
					if(gind.fitness instanceof KozaFitness)
						fitness = ((KozaFitness)fit).standardizedFitness();

					if(fitness < bestSubPopFitness)
					{
						bestSubPopFitness = fitness;
					}
					fitnessSum += fitness;
				}

				// 4. post subpopulation iteration operations

				// 4.2 update global statistics and save it to file.
				if(bestSubPopFitness < bestGenerationFitness)
				{
					bestGenerationFitness = bestSubPopFitness;
					bestGeneration = state.generation;
				}

				state.output.println(state.generation + ",\t"
						+ ReactiveGPHHProblem.getNumEvaluation()
						+ ",\t" + fitnessSum / state.population.subpops[0].individuals.length
						+ ",\t" + bestSubPopFitness, statLogID);
				state.output.flush();
			}

			// 5. post population iteration statistics.

			// 5.1 close the file.
			writer.close();

		} catch (IOException e)
		{
			state.output.fatal(e.toString());
		}
	}

	public String generatePopulationFileName(int generation)
	{
		return genPopFileName + "." + generation + ".bin";
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		try
		{
			super.setup(state, base);

			genPopFileName = state.parameters.getStringWithDefault(
					base.push(P_GEN_POP_FILE_NAME),	null, "init-pop.dat");

			saveGenerations = state.parameters.getBoolean(base.push(P_SAVE_POP), null, false);

			File statLogFile = new File(genPopFileName + ".statistics");
			if(statLogFile.exists())
			{
				statLogFile.delete();
			}
			statLogFile.createNewFile();
			statLogID = state.output.addLog(statLogFile, false, false);

		} catch (IOException e)
		{
			state.output.fatal("Failed to create the file for storing initial population.");
		}
	}

	@Override
	public void postEvaluationStatistics(EvolutionState state)
	{
		super.postEvaluationStatistics(state);
		if(saveGenerations)
		{
			iteratePopulation(state);
		}
	}
}
