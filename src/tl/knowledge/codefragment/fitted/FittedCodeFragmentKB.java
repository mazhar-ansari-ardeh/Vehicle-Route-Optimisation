package tl.knowledge.codefragment.fitted;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.Fitness;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPProblem;
import ec.gp.GPTree;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKB;

@Deprecated
public class FittedCodeFragmentKB extends CodeFragmentKB
{
	private GPProblem problem = null;
	private EvolutionState state = null;
	private Fitness fitnessPrototype = null;

	private int tournamentSize = 0;

	protected ArrayList<FittedCodeFragment> repository = new ArrayList<>();

	/**
	 * Constructs a new knowledge base object
	 * @param state The <code>EvolutionState</code> object that is running the algorithm.
	 * @param problem The problem that will be used to find fitness of code fragments.
	 * @param fitnessPrototype The fitness object that will be used for finding the fitness of
	 * code fragments. The object is used as a prototype and the object will be used directly.
	 * @param tournamentSize The tournament size for selecting code fragments.
	 */
	public FittedCodeFragmentKB(EvolutionState state, GPProblem problem,
			Fitness fitnessPrototype, int tournamentSize)
	{
		if (state == null)
			throw new NullPointerException("State cannot be null");

		if(problem == null)
			throw new NullPointerException("Problem cannot be null");

		if (fitnessPrototype == null)
			throw new NullPointerException("Fitness cannot be null");

		if(tournamentSize <= 0)
			throw new IllegalArgumentException("Tournament size must be greater than zero");

		this.state = state;
		this.problem = problem;
		this.fitnessPrototype = fitnessPrototype;
		this.tournamentSize = tournamentSize;
	}

	public class TournamentExtractor implements KnowledgeExtractor
	{
		@Override
		public boolean hasNext()
		{
			return !repository.isEmpty();
		}

		@Override
		public FittedCodeFragment getNext()
		{
			if(repository.isEmpty())
				return null;

			int best = state.random[0].nextInt(repository.size());
			for (int i = 1; i < tournamentSize; i++)
			{
				int selected = state.random[0].nextInt(repository.size());
				if(repository.get(selected).getFitness() < repository.get(best).getFitness())
					best = selected;
			}

			repository.get(best).incrementCounter();
			return repository.get(best);
		}

		@Override
		public void reset()
		{
			// Do nothing.
		}
	} // class TournamentExtractor


	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return new TournamentExtractor();
	}


	@Override
	public boolean addItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		// TODO: It is much better to use builders, initializers and species to create a new ind.
		GPIndividual ind = new GPIndividual();
		ind.evaluated = false;
		ind.fitness = (Fitness) fitnessPrototype.clone();
		ind.trees = new GPTree[1];
		ind.trees[0] = new GPTree();
		ind.trees[0].owner = ind;
		ind.trees[0].child = item;
		item.parent = ind.trees[0].child;
		item.argposition = 0;

		problem.evaluate(state, ind, 0, 0);
		item.parent = null;

		double dfitness = ind.fitness.fitness();
		repository.add(new FittedCodeFragment(item, dfitness));

		return true;
	}

	@Override
	public boolean removeItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		for(int i = 0; i < repository.size(); i++)
		{
			if(repository.get(i).getItem().equals(item)) // TODO: Does the equal function work well here?
			{
				repository.remove(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		for(int i = 0; i < repository.size(); i++)
			if(repository.get(i).getItem().equals(item)) // TODO: Does the equal function work well here?
				return true;

		return false;
	}
}
