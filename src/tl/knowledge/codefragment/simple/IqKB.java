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
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractionMethod;
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
public class IqKB extends CodeFragmentKB
{
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	/**
	 * Maximum depth of code fragments to be extracted from source. Code fragments with depth
	 * greater than this value will be ignored.
	 */
	private double cfMaxDepth;

	private EvolutionState state;

	public IqKB()
	{
		super();
	}

	public IqKB(EvolutionState state, int threadnum, int cfMaxDepth)
	{
		if(cfMaxDepth > 100)
			throw new IllegalArgumentException("K is a percentage value and cannot be greater "
					+ "than 100");
		this.cfMaxDepth = cfMaxDepth;
		this.state = state;
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
		// Only top half individuals will be selected.
		int sampleSize = Math.round(0.5f * p.subpops[0].individuals.length);
		state.output.warning("Sample size in IqKB.addFrom: " + sampleSize);
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

		boolean added = false;
		ArrayList<GPNode> allNodes = TreeSlicer.sliceAllToNodes(gpIndividual, false);
		for(GPNode node : allNodes)
		{
			if(node.depth() <= cfMaxDepth)
				added |= addItem(node);
		}

		return added;
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

	/**
	 * Returns a knowledge extractor for this object. This class only supports the
	 * {@code RandomKnowledgeExtractor} class.
	 *
	 * @return an instance of the {@code RandomKnowledgeExtractor} class.
	 */
	@Override
	public SinglePassKnowledgeExtractor<GPNode> getKnowledgeExtractor()
	{
		return new SinglePassKnowledgeExtractor<GPNode>(repository);
	}
}
