package tutorial5;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import ec.EvolutionState;
import ec.util.Parameter;

/**
 * A finisher class, subtype of SimpleFinisher, that saves a copy of the population
 * of the final generation to a file.
 * @author mazhar
 */
public class PopulationSaverFinisher extends ec.simple.SimpleFinisher
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final String FILE_NAME = "final_population";

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
	}

	@Override
	public void finishPopulation(EvolutionState state, int result)
	{
		File file = new File(FILE_NAME);
		PrintWriter writer = null;
		try
		{
			if(file.exists())
			{
				file.delete(); // Always have a new instance.
				file.createNewFile();
			}
			writer = new PrintWriter(file);
			state.population.printPopulation(state, writer);
		} catch (IOException e)
		{
			state.output.error("Failed to write population to a file. Exp: " + e.toString());
		}
		finally {
			if(writer != null)
				writer.close();
		}
		super.finishPopulation(state, result);
	}
}
