package tutorial7.knowledge.codefragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPSpecies;
import tutorial7.TreeSlicer;
import tutorial7.knowledge.KnowledgeExtractor;
import tutorial7.knowledge.KnowledgeItem;
import tutorial7.knowledge.KnowlegeBase;

public class CodeFragmentKB implements KnowlegeBase<GPNode>
{
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	/**
	 * Adds a new item to the repository. If the repository contains the given item,
	 * the method will not modify the knowledge base and returns <code>false</code>.
	 *
	 * @param item The item to be stored into this knowledge base. If this parameter
	 * is <code>null</code> or is not an instance of <code>CodeFragmentKI</code>, the
	 * method will ignore them and return <code>false</code>.
	 *
	 * @return If the item is added successfully the return value will be <code>true
	 * </code> and otherwise, it will be <code>false</code>.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean addItem(KnowledgeItem<GPNode> item)
	{
		if (item == null || !(item instanceof CodeFragmentKI))
		{
			return false;
		}

		if(repository.containsKey(item.hashCode()))
			return false;
		repository.put(item.hashCode(), (CodeFragmentKI) item);

		return true;
	}


	/**
	 * Extracts code fragments from the given <code>gpIndividual</code> and adds them to this base.
	 * In the context of this class, a code fragment is a child of the root node of the given
	 * <code>gpIndividual</code> object.
	 * @param gpIndividual A <code>GPIndividual</code> object from which code fragments will be
	 * extracted and added to this base. If <code>gpIndividual</code> is <code>null</code>, it will
	 * be ignored.
	 * @return <code>true</code> if the function added items from <code>gpIndividual</code> to this
	 * base and <code>false</code> otherwise.
	 */
	public boolean addFrom(GPIndividual gpIndividual)
	{
		if (gpIndividual == null)
		{
			return false;
		}

		ArrayList<GPNode> nodes = TreeSlicer.sliceToNodes(gpIndividual, false);
		nodes.forEach(node ->
			{
				CodeFragmentKI item = new CodeFragmentKI(node);
				repository.put(item.hashCode(), item);
			});

		return !nodes.isEmpty();
	}

	/**
	 * Extracts code fragments from the given <code>Population</code> object and adds them to this
	 * base. To do this, the function extracts code fragments from each individual from every
	 * sub-population of the given population. In the context of this class, a code fragment is a
	 * child of the root node of the given <code>gpIndividual</code> object.
	 * @param population A <code>Population</code> object from which code fragments will be
	 * extracted and added to this base. If <code>population</code> is <code>null</code>, it will
	 * be ignored.
	 * @return <code>true</code> if the function added items from <code>population</code> to this
	 * base and <code>false</code> otherwise.
	 */
	public boolean addFrom(Population population)
	{
		if (population == null)
		{
			return false;
		}

		boolean added = false;
		for(Subpopulation sub : population.subpops)
		{
			if(!(sub.species instanceof GPSpecies)
			|| !(sub.species.i_prototype instanceof GPIndividual))
				continue;
			for(Individual ind : sub.individuals)
			{
				if( addFrom((GPIndividual)ind) == true)
					added = true;
			}
		}

		return added;
	}

	/**
	 * Extracts code fragments that are stored inside the given <code>file</code> object and adds
	 * them to this base. To do this, the function extracts code fragments from each individual that
	 * are stored inside the given file. The function assumes that first, number of sub-populations
	 * is written and also, it assumes that for each sub-population, first, the number of
	 * individuals in the population is written. If the file contains objects that are not of the
	 * type <code>GPIndividual</code>, it will ignore the object.
	 * In the context of this class, a code fragment is a child of the root node of the given
	 * <code>gpIndividual</code> object.
	 * @param population A <code>Population</code> object from which code fragments will be
	 * extracted and added to this base. If <code>population</code> is <code>null</code>, it will
	 * be ignored.
	 * @return <code>true</code> if the function added items from <code>population</code> to this
	 * base and <code>false</code> otherwise.
	 */
	public boolean addFrom(File file, EvolutionState state)
	{
		if (file == null)
		{
			return false;
		}

		boolean added = false;
		try(ObjectInputStream dis = new ObjectInputStream(new FileInputStream(file)))
		{
			int nsub = dis.readInt();
			for(int i = 0; i < nsub; i++)
			{
				int nind = dis.readInt();
				for(int j = 0; j < nind; j++)
				{
					Object ind = dis.readObject();
					if(!(ind instanceof GPIndividual))
						continue;

					if(addFrom((GPIndividual) ind))
						added = true;
				}
			}

			return added;
		} catch (FileNotFoundException e)
		{
			return false;
		} catch (IOException e)
		{
			return added;
		} catch (ClassNotFoundException e)
		{
			// e.printStackTrace();
			return added;
		}
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
	public boolean removeItem(KnowledgeItem<GPNode> item)
	{
		if (item == null || !(item instanceof CodeFragmentKI))
		{
			return false;
		}

		return repository.remove(item.hashCode()) != null;
	}

	/**
	 * Checks if this knowledge base contains a given <code>KnowledgeItem</code> or
	 * not.
	 *
	 * @param item An item to be checked. If the parameter is <code>null</code> or
	 * is not an instance of <code>CodeFragmentKB</code>, the method ignores it
	 * and returns <code>false</code>.
	 *
	 * @return <code>true</code> if this knowledge base contains the item and
	 * <code>false</code> otherwise.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean contains(KnowledgeItem<GPNode> item)
	{
		if (item == null || !(item instanceof CodeFragmentKI))
		{
			return false;
		}

		return repository.containsKey(item.hashCode());
	}

	@Override
	public boolean isEmpty()
	{
		return repository.size() == 0;
	}

	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return new CyclicCodeFragmentKnowledgeExtractor();
	}

	public class CyclicCodeFragmentKnowledgeExtractor implements KnowledgeExtractor
	{
		Iterator<Integer> iter;

		public CyclicCodeFragmentKnowledgeExtractor()
		{
			 iter = repository.keySet().iterator();
		}

		@Override
		public boolean hasNext()
		{
			return !repository.isEmpty();
		}

		@Override
		public KnowledgeItem<GPNode> getNext()
		{
			if(iter.hasNext() == false)
				iter = repository.keySet().iterator();

			return repository.get(iter.next());
		}

		@Override
		public void reset()
		{
			// Do nothing. This extractor is cyclic and reseting does not apply to it.
		}
	}

}
