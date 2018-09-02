package tutorial7;

import java.io.PrintWriter;

import ec.EvolutionState;
import ec.util.Parameter;

/**
 * A finisher class, subtype of SimpleFinisher, that saves a copy of the population
 * of the final generation to a destination. The class is abstract and classes that
 * inherit from it need to implement <code>getDestination</code> to provide a destination
 * for saving data. 
 * @author mazhar
 */
public abstract class PopSaverFinisher extends ec.simple.SimpleFinisher
{
	private static final long serialVersionUID = 1L;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);
	}
	
	public abstract PrintWriter getDestination();
	
	/**
	 * If it returns <code>true</code>, the class will close the destination stream after
	 * the population is saved to it. 
	 * @return <code>true</code> if the class should close the destination stream after 
	 * it is finished with saving to it. 
	 */
	public abstract boolean shouldCloseDestination();

	@Override
	public void finishPopulation(EvolutionState state, int result)
	{
		PrintWriter writer = getDestination();
		state.population.printPopulation(state, writer);
		writer.flush();
		
		if(shouldCloseDestination())
		{
			writer.close();
		}

		super.finishPopulation(state, result);
	}
}
