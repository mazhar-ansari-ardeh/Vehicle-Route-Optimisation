package tutorial7.knowledge.codefragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPSpecies;
import tutorial7.gp.TreeSlicer;
import tutorial7.knowledge.KnowledgeItem;
import tutorial7.knowledge.KnowlegeBase;

public abstract class CodeFragmentKB implements KnowlegeBase<GPNode>
{

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

		return addItem(item.getItem());
	}

	/**
	 * Add a new item to this knowledge base. This method is the interface to the underlying storage
	 * that actually stores the knowledge.
	 *
	 * @param item The item to be stored into this knowledge base. If this parameter
	 * is <code>null</code> the method should ignore it and return <code>false</code>. However,
	 * this is not a hard limitation and implementors may fins a use for it.
	 *
	 * @return If the item is added successfully the return value will be <code>true
	 * </code> and otherwise, it will be <code>false</code>.
	 *
	 * @author Mazhar
	 */
	public abstract boolean addItem(GPNode item);

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
//		for(GPNode node : nodes)
//			addItem(node);
		nodes.forEach(node ->
			{
				addItem(node);
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
	 * Removes a given code fragment, as an instance of <code>GPNode</code>, from knowledge base.
	 *
	 * @param item An item to be removed. If the parameter is <code>null</code> or
	 * is not an instance of <code>CodeFragmentKB</code>, the method should ignore it
	 * and return <code>false</code>.
	 *
	 * @return <code>true</code> if the item is successfully removed from this
	 * knowledge base and <code>false</code> otherwise.
	 *
	 * @author Mazhar
	 */
	public abstract boolean removeItem(GPNode item);

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

		return removeItem(item.getItem());
	}

	/**
	 * Removes the given code fragment, as an instance of <code>KnowledgeItem</code>, from
	 * knowledge base.
	 *
	 * @param item An item to be removed. If the parameter is <code>null</code>, the method
	 * should ignore it and return <code>false</code>.
	 *
	 * @return <code>true</code> if the item is successfully removed from this
	 * knowledge base and <code>false</code> otherwise.
	 *
	 * @author Mazhar
	 */
	public abstract boolean contains(GPNode item);


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

		return removeItem(item.getItem());
	}
}
