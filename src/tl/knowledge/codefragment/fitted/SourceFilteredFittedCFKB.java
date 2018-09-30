package tl.knowledge.codefragment.fitted;

import java.util.Arrays;
import java.util.PriorityQueue;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;

public class SourceFilteredFittedCFKB extends FittedCodeFragmentKB
{
	int filterSize = 0;

	/**
	 * Creates a new instance of <code>SourceFilteredfittedCFKB</code>.
	 * @param state The evolution state object that is creating this object.
	 * @param problem The target problem that will be used to rank code fragments
	 * @param tournamentSize The tournament size that will be used for selecting code fragments from
	 * this knowledge base.
	 * @param filterSize Filter size. When a population of individuals is given to this knowledge
	 * base to be added, only this number of individuals with best fitness value will be added to
	 * this knowledge base and the rest will be ignored.
	 */
	public SourceFilteredFittedCFKB(EvolutionState state, GPProblem problem, Fitness fitness,
			int tournamentSize, int filterSize)
	{
		super(state, problem, fitness, tournamentSize);

		if(filterSize <= 0)
			throw new IllegalArgumentException("Filter size must be greater than zero.");

		this.filterSize = filterSize;
	}

	/**
	 * Receives a population of individuals and adds codes fragments from them. This function
	 * implements the filtering logic and only selects the top <code>filterSize</code> individuals
	 * in term of their fitness. This method considers all subpopulations and
	 * assumes that all subpopulations are of type <code>GPIndividuals</code> and fitness of
	 * individuals is of type <code>KozaFitness</code>.
	 * @param population The population of individuals from which code fragments will be extracted.
	 * If this parameter is <code>null</code>, the return value will be <code>false</code>.
	 * @return <code>true</code> if at least one code fragment is extracted and added and
	 * <code>false</code>.
	 */
	@Override
	public boolean addFrom(Population population)
	{

		PriorityQueue<Individual> q = new PriorityQueue<>((Individual i1, Individual i2)->
		{
			GPIndividual ind1 = (GPIndividual) i1;
			GPIndividual ind2 = (GPIndividual) i2;

			return Double.compare(ind1.fitness.fitness(), ind2.fitness.fitness());
		});
		for(Subpopulation sub : population.subpops)
			q.addAll(Arrays.asList(sub.individuals));

		boolean added = false;
		for(int i = 0; i < filterSize; i++)
			added |= addFrom((GPIndividual) q.poll());

		return added;
	}

}
