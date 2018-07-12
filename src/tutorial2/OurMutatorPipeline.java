package tutorial2;

import ec.vector.*;
import ec.*;
import ec.util.*;

public class OurMutatorPipeline extends BreedingPipeline /* BreedingPipeline indicates that it is a non-leaf node*/
{
	private static final long serialVersionUID = 1L;

	//used only for our default base
	public static final String P_OURMUTATION = "our-mutation";

	public static final int NUM_SOURCES = 1;

	@Override
	public Parameter defaultBase() 
	{
		return VectorDefaults.base().push(P_OURMUTATION); 
	}

	@Override
	public int numSources() 
	{
		return NUM_SOURCES; 
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, 
			EvolutionState state, int thread) 
	{
		int n = sources[0].produce(min,max, start, subpopulation,inds,state,thread);

		// should we bother?
		if (!state.random[thread].nextBoolean(likelihood))
		{
			return n;
		}

		// Check to make sure that the individuals are IntegerVectorIndividuals and
		// grab their species.  For efficiency's sake, we assume that all the 
		// individuals in inds[] are the same type of individual and that they all
		// share the same common species -- this is a safe assumption because they're 
		// all breeding from the same subpopulation.
		if (!(inds[start] instanceof IntegerVectorIndividual)) 
			// Oh oh, wrong kind of individual
			state.output.fatal(
					"OurMutatorPipeline didn't get an " 
					+ "IntegerVectorIndividual.  The offending individual is "
					+ "in subpopulation " + subpopulation + " and it's:" + inds[start]);
		
		IntegerVectorSpecies species = (IntegerVectorSpecies)(inds[start].species);
		
        // mutate 'em!
		for(int q=start; q < n+start; q++)
        {
        	IntegerVectorIndividual i = (IntegerVectorIndividual)inds[q];
        	for(int x=0;x<i.genome.length;x++)
        		if (state.random[thread].nextBoolean(species.mutationProbability(x)))
        			i.genome[x] = -i.genome[x];
        	// it's a "new" individual, so it's no longer been evaluated
        	i.evaluated=false;
        }
        return n;
	}

}
