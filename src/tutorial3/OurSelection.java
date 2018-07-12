package tutorial3;

import ec.EvolutionState;
import ec.Individual;
import ec.SelectionMethod;
import ec.util.Parameter;

public class OurSelection extends SelectionMethod
{
	private static final long serialVersionUID = 1L;
	
    public static final String P_OURSELECTION = "our-selection";
    
    public static final String P_MIDDLEPROBABILITY = "middle-probability";  // our parameter name
    
    private double middleProbability;

	@Override
	public Parameter defaultBase() 
	{
		return new Parameter(P_OURSELECTION);
	}
	
	@Override
    public void setup(final EvolutionState state, final Parameter base)
    {
    	super.setup(state, base);
    	
    	Parameter defPar = defaultBase();
    	
    	middleProbability = state.parameters.getDoubleWithMax(base.push(P_MIDDLEPROBABILITY), 
    			defPar.push(P_MIDDLEPROBABILITY), 0, 1);
    	
    	if(middleProbability < 0.0)
    	{
    		state.output.fatal( "Middle-probability must be between 0.0 and 1.0" 
    						  , base.push(P_MIDDLEPROBABILITY), defPar.push(P_MIDDLEPROBABILITY));
    	}
    }

	/*
	 * A SelectionMethod's produce(...) method is supposed to return an individual from a subpopulation: 
	 * unlike Breeding Pipelines, Selection Method's produce(...) methods don't copy the individual first 
	 * -- they just return the original individual selected from the subpopulation. 
	 * Actually, SelectionMethods have two versions of the produce(...) method. One version returns N 
	 * individuals as requested. The second returns the index in the subpopulation array of a single selected 
	 * individual. SelectionMethod has a default implementation of the first method which just calls the second. 
	 * So here we will just write the second method (though we might override the first one as well, if we liked, 
	 * in order to make the system faster). 
	 */
	@Override
	public int produce(int subpopulation, EvolutionState state, int thread) 
	{
		if(state.random[thread].nextBoolean(middleProbability))
		{
			// pick three individuals, return the middle one. 
			Individual[] inds = state.population.subpops[subpopulation].individuals;
			int one = state.random[thread].nextInt(inds.length);
			int two = state.random[thread].nextInt(inds.length);
			int three = state.random[thread].nextInt(inds.length);
			if(inds[two].fitness.betterThan(inds[one].fitness))
			{
				if (inds[three].fitness.betterThan(inds[two].fitness)) //  1 < 2 < 3
                    return two;
				else if (inds[three].fitness.betterThan(inds[one].fitness)) //  1 < 3 < 2
                    return three;
				 else //  3 < 1 < 2
	                    return one;
			}
			else if (inds[three].fitness.betterThan(inds[one].fitness)) //  2 < 1 < 3
                return one;
            else if (inds[three].fitness.betterThan(inds[two].fitness)) //  2 < 3 < 1
                return three;
            else //  3 < 2 < 1
                return two;
		}
		else        //select a random individual's index
        {
			return state.random[thread].nextInt(
					state.population.subpops[subpopulation].individuals.length);
        }
	}
	
}
