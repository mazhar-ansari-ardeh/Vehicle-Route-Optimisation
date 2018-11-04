package tl.knowledge;

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
import tl.gp.KnowledgeExtractionMethod;
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.*;

/**
 * This class, with the class {@code SimpleCodeFragmentBuilder} implements the <i>SubTree</i> and
 * <i>FullTransfer</i> method that was proposed by Dinh et al. in their paper
 * "<i>Transfer Learning in Genetic Programming</i>". The selection between the <i>FullTransfer</i>
 * and <i>SubTree</i> methods is done through the {@code KnowledgeExtractionMethod} instance that
 * is passed to extraction methods. For a value of {@code KnowledgeExtractionMethod.Root}, the
 * <i>FullTransfer</i> method is selected. A value of {@code KnowledgeExtractionMethod.RootSubtree}
 * will select the <i>SubTree</i> method.
 * @author mazhar
 *
 */
public class MySimpleCodeFragmentKB extends CodeFragmentKB
{
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	private double k;

	EvolutionState state;


	public MySimpleCodeFragmentKB()
	{
		super();
	}

	public MySimpleCodeFragmentKB(EvolutionState state, int k)
	{
		if(k > 100)
			throw new IllegalArgumentException("K is a percentage value and cannot be greater "
					+ "than 100");
		this.k = k / 100f;
		this.state = state;
	}

	@Override
	public boolean addFrom(Population p, KnowledgeExtractionMethod method)
	{
		System.out.println("Inside SimpleCodeFragmentKB.addFrom");
		if (p == null)
		{
			System.out.println("Population is null. Exiting");
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
		state.output.warning("Sample size in MYSimpleCodeFragmentKB.addFrom: " + sampleSize);
		System.out.println("Sample size in MYSimpleCodeFragmentKB.addFrom: " + sampleSize);
		for(int i = 0; i < sampleSize; i++)
		{
			added |= addFrom((GPIndividual)p.subpops[0].individuals[i], method);
		}

		return added;
	}

	public boolean addFrom(GPIndividual gpIndividual, KnowledgeExtractionMethod method)
	{
		if (gpIndividual == null)
		{
			return false;
		}

		switch(method)
		{
		case Root:
			return addItem(gpIndividual.trees[0].child);
		case RootSubtree:
			ArrayList<GPNode> allNodes = TreeSlicer.sliceRootChildrenToNodes(gpIndividual, false);
			boolean added = false;
			for(GPNode node : allNodes)
				added |= addItem(node);
			// TODO: Put filters on extracted code fragments.
			return added;
		default:
			throw new IllegalArgumentException();
		}
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
		if(repository.containsKey(item.hashCode()))
			return false;

		repository.put(item.hashCode(), cfItem);


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

		return repository.remove(item.hashCode()) != null;
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

		return repository.containsKey(item.hashCode());
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
			return !repository.isEmpty();
		}

		@Override
		public CodeFragmentKI getNext()
		{
			if(iter.hasNext())
				return repository.get(iter.next());
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
