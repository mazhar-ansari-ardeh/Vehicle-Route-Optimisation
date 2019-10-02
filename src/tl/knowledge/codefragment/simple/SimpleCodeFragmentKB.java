package tl.knowledge.codefragment.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.*;

/**
 * This class, with the class {@code SimpleCodeFragmentBuilder} implements the <i>SubTree</i> and
 * <i>FullTransfer</i> method that was proposed by Dinh et al. in their paper
 * "<i>Transfer Learning in Genetic Programming</i>". The selection between the <i>FullTransfer</i>
 * and <i>SubTree</i> methods is done through the {@code KnowledgeExtractionMethod} instance that
 * is passed to extraction methods. For a value of {@code KnowledgeExtractionMethod.Root}, the
 * <i>FullTree</i> method is selected. A value of {@code KnowledgeExtractionMethod.RootSubtree}
 * will select the <i>SubTree</i> method.
 * @author mazhar
 *
 */
public class SimpleCodeFragmentKB extends CodeFragmentKB
{
	private boolean allowDuplicates;
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	private double k;

	EvolutionState state;


//	public SimpleCodeFragmentKB()
//	{
//		super();
//	}

	public SimpleCodeFragmentKB(EvolutionState state, int k, boolean allowDuplicates)
	{
		if(k > 100)
			throw new IllegalArgumentException("K is a percentage value and cannot be greater "
					+ "than 100");
		this.k = k / 100f;
		this.state = state;
		this.allowDuplicates = allowDuplicates;
	}

	@Override
	public boolean extractFrom(Population p, KnowledgeExtractionMethod method)
	{
		System.out.println("Inside SimpleCodeFragmentKB.addFrom");
		if (p == null)
		{
			state.output.fatal("Population is null. Exiting");
			return false;
		}

		Comparator<Individual> com2 = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(p.subpops[0].individuals, com2);

		boolean added = false;
		int sampleSize = (int) Math.round(k * p.subpops[0].individuals.length);
		state.output.warning("Sample size in SimpleCodeFragmentKB.addFrom: " + sampleSize);
		System.out.println("Sample size in SimpleCodeFragmentKB.addFrom: " + sampleSize);
		for(int i = 0; i < sampleSize; i++)
		{
			added |= extractFrom((GPIndividual)p.subpops[0].individuals[i], method);
		}

		return added;
	}

	public boolean extractFrom(GPIndividual gpIndividual, KnowledgeExtractionMethod method)
	{
		if (gpIndividual == null)
		{
			return false;
		}

		GPNode node = null;
		switch(method)
		{
		case Root:
			node = gpIndividual.trees[0].child;
			break;
		case RootSubtree:
			ArrayList<GPNode> allNodes = TreeSlicer.sliceRootChildrenToNodes(gpIndividual, false);
			if(allNodes.isEmpty())
				return false;;
			node = allNodes.get(state.random[0].nextInt(allNodes.size()));
			break;
		default:
			throw new IllegalArgumentException();
		}

		addItem(node, gpIndividual.fitness.fitness());

		return true;
	}


	/**
	 * Adds a new item to the repository. If the repository contains the given item,
	 * the method will not modify the knowledge base and returns <code>false</code>.
	 *
	 * @param item The item to be stored into this knowledge base. If this parameter
	 * is <code>null</code> the method will ignore them and return <code>false</code>.
	 *
	 * @return If the item is added successfully the return value will be <code>true
	 * </code> and otherwise, it will be <code>false</code>.
	 *
	 * @author Mazhar
	 */
	public boolean addItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		CodeFragmentKI cfItem = new CodeFragmentKI(item);
		if(!allowDuplicates && repository.containsKey(item.rootedTreeHashCode()))
		{
			state.output.warning("(Sub-)tree is already in the repository: " + item.makeCTree(false, true, true));
			return false;
		}
		repository.put(item.rootedTreeHashCode(), cfItem);

		return true;
	}

	public boolean addItem(GPNode item, double fitness)
	{
		if (item == null)
		{
			return false;
		}

		CodeFragmentKI cfItem = new CodeFragmentKI(item, fitness);
		if(!allowDuplicates && repository.containsKey(item.rootedTreeHashCode()))
		{
			state.output.warning("(Sub-)tree is already in the repository: " + item.makeCTree(false, true, true));
			return false;
		}
		repository.put(item.rootedTreeHashCode(), cfItem);

		return true;
	}


	/**
	 * Removes a given <code>KnowledgeItem</code> from knowledge base.
	 *
	 * @param item An item to be removed. If the parameter is <code>null</code> or
	 * is not an instance of <code>CodeFragmentKB</code>, the method ignores it
	 * and returns <code>false</code>.
	 *
	 * @return <code>true</code> if the item is successfully removed from this
	 * knowledge base and <code>false</code> otherwise.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean removeItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		return repository.remove(item.rootedTreeHashCode()) != null;
	}

	/**
	 * Checks if this knowledge base contains a given code fragment or not.
	 *
	 * @param item An item to be checked. If the parameter is <code>null</code>, the method ignores
	 * it and returns <code>false</code>.
	 *
	 * @return <code>true</code> if this knowledge base contains the item and <code>false</code>
	 * otherwise.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean contains(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		return repository.containsKey(item.rootedTreeHashCode());
	}

	/**
	 * Checks to see if this knowledge base is empty or not.
	 *
	 * @return <code>true</code> if the knowledge base is empty and <code>false</code> otherwise.
	 * @author Mazhar
	 */
	@Override
	public boolean isEmpty()
	{
		return repository.size() == 0;
	}

	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return new CodeFragmentKnowledgeExtractor();
	}

	public class CodeFragmentKnowledgeExtractor implements KnowledgeExtractor
	{
		Iterator<Integer> iter;

		public CodeFragmentKnowledgeExtractor()
		{
			 iter = repository.keySet().iterator();
		}

		@Override
		public boolean hasNext()
		{
			return iter.hasNext();
		}

		@Override
		public CodeFragmentKI getNext()
		{
			if(iter.hasNext())
			{
				CodeFragmentKI retval = repository.get(iter.next());
				retval.incrementCounter();
				return retval;
			}
			else
				return null;
		}

		@Override
		public void reset()
		{
			 iter = repository.keySet().iterator();
		}
	}
}
