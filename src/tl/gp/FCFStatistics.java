package tl.gp;

import java.io.File;
import java.io.IOException;

import ec.EvolutionState;
import ec.Fitness;
import ec.gp.GPIndividual;
import ec.gp.GPTree;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleStatistics;
import ec.util.Parameter;
import tl.problems.regression.one_d.RegressionProblem;


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

	/**
	 * The name that will be used to save population in generation. This is the base name and it
	 * will be augmented for each generation.
	 */
	private String genPopFileName = "";

	/**
	 * If <code>true</code>, population of GP will be saved after each generation.
	 */
	private boolean saveGenerations = false;


	/**
	 * If tree, the statistics will also contain a tree representation of the individuals in the
	 * population.The style of the saved representation is determined with ECJ's
	 * 'gp.tree.print-style' parameter.
	 */
	private boolean saveTree = false;

	/**
	 * Determines the style to use for saving tree representation of individuals in the population.
	 * This style is determined with ECJ's 'gp.tree.print-style' parameter.
	 */
	private String style = null;

	public final static String P_GEN_POP_FILE_NAME = "gen-pop-file";
	public final static String P_SAVE_POP = "save-pop";
	public final static String P_SAVE_POP_TREE = "save-tree";

	/**
	 * This function has two objectives: <p>
	 * 	1. Save the population of GP. <p>
	 * 	2. Save statistics of GP population. <p>
	 * @param state The <code>EvolutionState</code> object that is a representative of GP.
	 * @param popLogId The ID of the logger that will be used to save generation's population.
	 * @param statLogId The ID of the logger that will be used to save statistics of each
	 * 		  generation.
	 */
	private void savePopulation(EvolutionState state, int popLogId, int statLogId)
	{
		double bestFitness = Double.MAX_VALUE;
		double fitnessSum = 0;

		// iterate over individuals, save them and collect statistics.
		for(int i = 0; i < state.population.subpops[0].individuals.length; i++)
		{
			GPIndividual gind = (GPIndividual)state.population.subpops[0].individuals[i];
			Fitness fit = gind.fitness;
			double fitness = fit.fitness();
			if(gind.fitness instanceof KozaFitness)
				fitness = ((KozaFitness)fit).standardizedFitness();

			fitnessSum += fitness;
			if(fitness < bestFitness)
			{
				bestFitness = fitness;
			}

			String tree = "";
	        if (saveTree && style == null || style.equals(GPTree.V_LISP))
	        	// To draw the graph, use the command: dot -Tps filename.dot -o outfile.ps
	        	tree = gind.trees[0].child.makeGraphvizTree();
	        else if (saveTree && style.equals(GPTree.V_C))
	        	tree = gind.trees[0].child.makeCTree(true, true, true).replaceAll("\n", "") + ", ";
	        else if (saveTree && style.equals(GPTree.V_DOT))
	        	tree = gind.trees[0].child.makeGraphvizTree().replaceAll("\n", "") + ", ";
	        else if (saveTree && style.equals(GPTree.V_LATEX))
	        	tree = gind.trees[0].child.makeLatexTree().replaceAll("\n", "") + ", ";

	        if(i == 0)
	        	state.output.println("# index, " + (saveTree ? "tree, " : "")
	        						  + "fitness, STD fitness\n",
	        						  popLogId);

	        state.output.print( i + "\t, " + tree + "\t", popLogId);
			state.output.flush();
			state.output.println(", " + ", " + fitness, popLogId);
			state.output.println("", popLogId);
		} // for: Iterate over individuals

		if(state.generation == 0)
			state.output.println("# generation, mean, best", statLogId);

		state.output.println(state.generation + ",\t"
//							 + ((RegressionProblem)state.evaluator.p_problem).getEvaluationCount()
							 + ",\t" + fitnessSum / state.population.subpops[0].individuals.length
							 + ",\t" + bestFitness, statLogId);

		state.output.flush();
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

			saveTree = state.parameters.getBoolean(base.push(P_SAVE_POP_TREE), null, false);

			style = state.parameters.getString(new Parameter("gp.tree.print-style"), null);

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
			String fileName = genPopFileName + "." + state.generation + ".dat";
			File file = new File(fileName);
			try
			{
				if(file.exists())
				{
					file.delete();
				}
				file.createNewFile();

				int logId = state.output.addLog(file, false, false);
				savePopulation(state, logId, statLogID);
				state.output.removeLog(logId); // It is not needed anymore, remove it.
			}
			catch(IOException exp)
			{
				state.output.fatal("Failed to create log for " + fileName + ". Reason: "
									+ exp.toString());
			}
		}
	}
}
