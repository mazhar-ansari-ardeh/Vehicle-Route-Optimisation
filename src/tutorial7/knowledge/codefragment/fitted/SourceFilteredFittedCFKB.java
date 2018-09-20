package tutorial7.knowledge.codefragment.fitted;

import java.util.Arrays;
import java.util.PriorityQueue;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;

public class SourceFilteredFittedCFKB extends FittedCodeFragmentKB 
{
	int filterSize = 0;

	public SourceFilteredFittedCFKB(EvolutionState state, GPProblem problem, int tournamentSize, int filterSize) 
	{
		super(state, problem, tournamentSize);
		if(filterSize <= 0)
			throw new IllegalArgumentException("Filter size must be greater than zero.");
		this.filterSize = filterSize; 
	}
	
	@Override
	public boolean addFrom(Population population)
	{	
		PriorityQueue<Individual> q = new PriorityQueue<>((Individual i1, Individual i2)-> 
		{
			GPIndividual ind1 = (GPIndividual) i1;
			GPIndividual ind2 = (GPIndividual) i2;
			
			return Double.compare(
					((KozaFitness)ind1.fitness).standardizedFitness(),((KozaFitness)ind2.fitness).standardizedFitness());
		});
		for(Subpopulation sub : population.subpops)
			q.addAll(Arrays.asList(sub.individuals));
		
		boolean added = false; 
		for(int i = 0; i < filterSize; i++)
			added |= addFrom((GPIndividual) q.poll());
		
		return added; 
	}

}
