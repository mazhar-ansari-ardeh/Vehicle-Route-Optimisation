package tl.knowledge.codefragment.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * This class, with the class {@code TLGPCriptorBuilder} implements the <i>TLGP-Criptor</i> and
 * method that was proposed by Igbal et al. in their paper
 * "<i>Reusing Extracted Knowledge in Genetic Programming to Solve Complex Texture Image
 * Classification Problems</i>".
 * @author mazhar
 *
 */
public class TLGPCriptorKB extends CodeFragmentKB
{
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

//	private double knowledgeProbability;

	EvolutionState state;


	public TLGPCriptorKB()
	{
		super();
	}

	public TLGPCriptorKB(EvolutionState state)
	{
//		if(knowledgeProbability > 1)
//			throw new IllegalArgumentException("Knowledge probability must be a value between 0 and"
//					+ " 1.");
//		this.knowledgeProbability = knowledgeProbability;
		this.state = state;
	}

	@Override
	public boolean extractFrom(Population p, KnowledgeExtractionMethod method)
	{
		System.out.println("Inside SimpleCodeFragmentKB.addFrom");
		if (p == null)
		{
			System.out.println("Population is null. Exiting");
			return false;
		}

		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(p.subpops[0].individuals, comp);

		boolean added = false;
		int sampleSize = p.subpops[0].individuals.length / 2;
		state.output.warning("Sample size in TLGPCriptorKB.addFrom: " + sampleSize);
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

		switch(method)
		{
		case RootSubtree:
			ArrayList<GPNode> allNodes = TreeSlicer.sliceRootChildrenToNodes(gpIndividual, false);
			boolean added = false;
			for(GPNode node : allNodes)
				added |= addItem(node);
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
		if(repository.containsKey(cfItem.hashCode()))
		{
			repository.get(cfItem.hashCode()).incrementDuplicateCount();
			return false;
		}
		repository.put(cfItem.hashCode(), cfItem);


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
		return new RandomKnowledgeExtractor<GPNode>(repository, state.random[0]);
	}
}
