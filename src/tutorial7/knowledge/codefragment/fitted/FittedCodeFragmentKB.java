package tutorial7.knowledge.codefragment.fitted;

import java.util.ArrayList;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPProblem;
import ec.gp.GPTree;
import ec.gp.koza.KozaFitness;
import tutorial7.knowledge.KnowledgeExtractor;
import tutorial7.knowledge.codefragment.CodeFragmentKB;

public class FittedCodeFragmentKB extends CodeFragmentKB
{
	private GPProblem problem = null;
	private EvolutionState state = null;

	private int tournamentSize = 0;

	private ArrayList<FittedCodeFragment> repository = new ArrayList<>();

	public FittedCodeFragmentKB(EvolutionState state, GPProblem problem, int tournamentSize)
	{
		if (state == null)
		{
			throw new NullPointerException("State cannot be null");
		}

		if(problem == null)
			throw new NullPointerException("Problem cannot be null");

		if(tournamentSize <= 0)
			throw new IllegalArgumentException("Tournament size must be greater than zero");

		this.problem = problem;
		this.state = state;
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

			int best = 0;
			for (int i = 1; i < tournamentSize; i++)
			{
				int selected = state.random[0].nextInt(repository.size());
				if(repository.get(selected).fitness	< repository.get(best).fitness)
					best = selected;
			}

			repository.get(best).increaseCounter();
			return repository.get(best);
		}

		@Override
		public void reset()
		{
			// Do nothing.
		}
	}


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
		ind.fitness = new KozaFitness();
		ind.trees = new GPTree[1];
		ind.trees[0] = new GPTree();
		ind.trees[0].owner = ind;
		ind.trees[0].child = item;
		item.parent = ind.trees[0].child;
		item.argposition = 0;

		problem.evaluate(state, ind, 0, 0);

		// TODO: Check the value returned by the fitness() function.
		repository.add(new FittedCodeFragment(item, ind.fitness.fitness()));

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
