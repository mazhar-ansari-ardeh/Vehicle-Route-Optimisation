package tutorial7.gp;

import java.io.File;
import java.io.IOException;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPTree;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleStatistics;
import ec.util.Parameter;


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

	private int initPopLogID;

	public final static String P_INIT_POP_FILE_NAME = "init-pop-file";

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		try
		{
			super.setup(state, base);
			String fileName = state.parameters.getStringWithDefault(base.push(P_INIT_POP_FILE_NAME),
					null, "init-pop.dat");

			File logFile = new File(fileName);
			if(logFile.exists())
			{
				logFile.delete();

				logFile.createNewFile();
			}
			initPopLogID = state.output.addLog(logFile, false, false);
		} catch (IOException e)
		{
			state.output.fatal("Failed to create the file for storing initial population.");
		}
	}

	@Override
	public void postInitializationStatistics(EvolutionState state)
	{
		super.postInitializationStatistics(state);
		// Print format: index, tree graph, fitness, hits, standard fitness
		String style = state.parameters.getString(new Parameter("gp.tree.print-style"), null);
		for(int i = 0; i < state.population.subpops[0].individuals.length; i++)
		{
			GPIndividual gind = (GPIndividual)state.population.subpops[0].individuals[i];
			double fitness = gind.fitness.fitness();
			String tree = "";
	        if (style == null || style.equals(GPTree.V_LISP))
	        	// To draw the graph, use the command: dot -Tps filename.dot -o outfile.ps
	        	tree = gind.trees[0].child.makeGraphvizTree();
	        else if (style.equals(GPTree.V_C))
	        	tree = gind.trees[0].child.makeCTree(true, true, true);
	        else if (style.equals(GPTree.V_DOT))
	        	tree = gind.trees[0].child.makeGraphvizTree();
	        else if (style.equals(GPTree.V_LATEX))
	        	tree = gind.trees[0].child.makeLatexTree();

	        tree = tree.replaceAll("\n", "");

	        state.output.print( i + "\t, " + tree + ", \t" + fitness, initPopLogID);
			state.output.flush();
			if(gind.fitness instanceof KozaFitness)
			{
				KozaFitness kfit = (KozaFitness)gind.fitness;
				double hits = kfit.hits;
				double stdFit = kfit.standardizedFitness();
				state.output.println(", " + hits + ", " + stdFit, initPopLogID);
			}
			state.output.println("", initPopLogID);
		}
		state.output.flush();
		state.output.removeLog(initPopLogID); // It is not needed anymore, remove it.
	}
}
