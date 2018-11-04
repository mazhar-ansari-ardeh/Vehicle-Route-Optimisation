package tl.knowledge.codefragment.fitted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPProblem;
import ec.gp.GPTree;
import tl.gp.KnowledgeExtractionMethod;
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKB;

public class SourceFilteredFittedCFKB extends CodeFragmentKB
{
	int filterSize = 0;

	private ArrayList<DoubleFittedCodeFragment> repository = new ArrayList<>();

	private EvolutionState state;

	private GPProblem targetProblem;

	private Fitness fitnessPrototype;

	private int tournamentSize;

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
		super();
		// super(state, problem, fitness, tournamentSize);

		if (state == null)
			throw new NullPointerException("State cannot be null");

		if(problem == null)
			throw new NullPointerException("Problem cannot be null");

		if (fitness == null)
			throw new NullPointerException("Fitness cannot be null");

		if(tournamentSize <= 0)
			throw new IllegalArgumentException("Tournament size must be greater than zero");

		this.state = state;
		this.targetProblem = problem;
		this.fitnessPrototype = fitness;
		this.tournamentSize = tournamentSize;

		if(filterSize <= 0)
			throw new IllegalArgumentException("Filter size must be greater than zero.");
		this.filterSize = filterSize;
	}


	@Override
	public boolean extractFrom(File file, KnowledgeExtractionMethod method)
	{
		if(method != KnowledgeExtractionMethod.ExactCodeFragment)
			return super.extractFrom(file, method);

		PriorityQueue<DoubleFittedCodeFragment> q = new PriorityQueue<>(
				(DoubleFittedCodeFragment cf1, DoubleFittedCodeFragment cf2) ->
				{
					return Double.compare(cf1.getFitnessOnSource(), cf2.getFitnessOnSource());
				}
				);
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)))
		{
			while(true)
			{
				Object obj = ois.readObject();
				if(!(obj instanceof DoubleFittedCodeFragment))
				{
					state.output.warning("Loaded an object that is not instance of "
							+ "`DoubleFittedCodeFragment`: " + obj.getClass().toString());
					continue;
				}
				q.add((DoubleFittedCodeFragment) obj);
			}
		} catch (FileNotFoundException e)
		{
			state.output.fatal("Knowledge file not found: " + e.toString());
		}
		catch (ClassNotFoundException e)
		{
			state.output.fatal("Class not found?: " + e.toString());
		}
		catch (IOException e)
		{
			System.out.println(e.toString());
			// This is part of the implementation logic. Since the file does not contain any counter
			// for the number of items in it, end of file IOException is taken as a marker of the
			// end. It is not best possible solution but well, I am in a hurry.
			// e.printStackTrace();
		}
		if(q.isEmpty())
			state.output.fatal("Knowledge file is empty.");

		boolean added = false;
		for(int i = 0; i < filterSize; i++)
		{
			DoubleFittedCodeFragment cf = q.poll();
			if(cf == null)
			{
				state.output.warning("Knowledge file contains fewer items than filter size.");
				return added;
			}
			added |= repository.add(cf);
		}

		return added;
	}


	/**
	 * Receives a population of individuals and adds codes fragments from them. This function
	 * implements the filtering logic and only selects the top <code>filterSize</code> individuals
	 * in term of their fitness. This method considers all subpopulations and
	 * assumes that all subpopulations are of type <code>GPIndividuals</code> and fitness of
	 * individuals is of type <code>KozaFitness</code>.
	 * @param population The population of individuals from which code fragments will be extracted.
	 * If this parameter is <code>null</code>, the return value will be <code>false</code>. Because
	 * this population is assumed to come from the source problem, fitness value of individuals in
	 * this population is taken as fitness on source domain and therefore, code fragments that are
	 * extracted from individuals in this population are evaluated again on target problem when
	 * needed.
	 * @param method
	 * @return <code>true</code> if at least one code fragment is extracted and added and
	 * <code>false</code>.
	 */
	@Deprecated // TODO: Maybe it is better to throw a NotSupported exception.
	@Override
	public boolean addFrom(Population population, KnowledgeExtractionMethod method)
	{
		if(method == KnowledgeExtractionMethod.ExactCodeFragment)
			throw new IllegalArgumentException("This extraction method is not supported: "
											   + method);
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
			added |= addFrom((GPIndividual) q.poll(), method);

		return added;
	}


	/**
	 * Receives an individuals and adds codes fragments from it. This function
	 * implements the filtering logic and only selects the top <code>filterSize</code> individuals
	 * in term of their fitness. This method considers the fitness value of the individual as the
	 * fitness value of code fragments.
	 * @param gpIndividual The individual from which code fragments will be extracted.
	 * If this parameter is <code>null</code>, the return value will be <code>false</code>. Because
	 * this individual is assumed to come from the source problem, fitness value of it is taken as
	 * fitness on source domain and therefore, code fragments that are
	 * extracted from individuals in this population are evaluated again on target problem when
	 * needed.
	 * @param method
	 * @return <code>true</code> if at least one code fragment is extracted and added and
	 * <code>false</code>.
	 */
	@Deprecated
	@Override
	public boolean addFrom(GPIndividual gpIndividual, KnowledgeExtractionMethod method)
	{
		if(method == KnowledgeExtractionMethod.ExactCodeFragment)
			throw new IllegalArgumentException("This extraction method is not supported: "
												+ method);

		if (gpIndividual == null)
		{
			return false;
		}

		ArrayList<GPNode> nodes = null;
		switch(method)
		{
		case AllSubtrees:
			nodes = TreeSlicer.sliceAllToNodes(gpIndividual, false);
			break;
		case RootSubtree:
			nodes = TreeSlicer.sliceRootChildrenToNodes(gpIndividual, false);
			break;
		default:
			state.output.fatal("This method does not support the given extraction method: "
					+ method);
		}

		// TODO: Also consider depth limit.
		nodes.forEach(node ->
		{
			repository.add(new DoubleFittedCodeFragment(node, gpIndividual.fitness.fitness(), null));
		});

		return !nodes.isEmpty();
	}


	public class TournamentExtractor implements KnowledgeExtractor
	{
		@Override
		public boolean hasNext()
		{
			return !repository.isEmpty();
		}

		@Override
		public DoubleFittedCodeFragment getNext()
		{
			if(repository.isEmpty())
				return null;

			int best = state.random[0].nextInt(repository.size());
			for (int i = 1; i < tournamentSize; i++)
			{
				int selected = state.random[0].nextInt(repository.size());
				DoubleFittedCodeFragment selectedCF = repository.get(selected);
				DoubleFittedCodeFragment bestCF = repository.get(best);

				double selectFitness = getFitnessOnTarget(selectedCF);
				double bestFitness = getFitnessOnTarget(bestCF);

				if(selectFitness < bestFitness)
					best = selected;
			}

			repository.get(best).increaseCounter();
			return repository.get(best);
		}

		private double getFitnessOnTarget(DoubleFittedCodeFragment cf)
		{
			Double fitness = cf.getFitnessOnTarget();
			if(fitness == null)
			{
				GPIndividual ind = cf.asIndividual(fitnessPrototype);
				targetProblem.evaluate(state, ind, 0, 0);
				fitness = ind.fitness.fitness();
				cf.setFitnessOnTarget(fitness);
			}

			return fitness;
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


	/**
	 * Adds an item to this knowledge base. This method does not have access to source domain and as
	 * a result, it will consider the fitness of the item in target domain also as its fitness on
	 * source domain. It is better not to use this method unless there is a very good reason that
	 * considers this fact.
	 * @param item an item to be added to this knowledge base. This method will ignore the item if
	 * it is {@code null}.
	 * @return {@code true} if the given item is added to repository and {@code false} otherwise.
	 */
	@Deprecated
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

		targetProblem.evaluate(state, ind, 0, 0);
		item.parent = null;

		double dfitness = ind.fitness.fitness();
		repository.add(new DoubleFittedCodeFragment(item, dfitness, dfitness));

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
