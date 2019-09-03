package tl.gp.hash;

import ec.gp.GPIndividual;
import ec.gp.GPNode;

/**
 * Defines the interface for any hash calculating algorithm.
 */
public interface HashCalculator
{
    /**
     * Calculates the hash value of a GP (sub-)tree whose root node is given.
     * @param tree the root node of a GP (sub-)tree
     * @return the hash value of the given GP (sub-)tree
     */
    int hashOfTree(GPNode tree);

    /**
     * Calculates the hash value of a GP tree of the given GP individual.
     * @param ind the GP individual that contains GP trees
     * @param tree the index of the GP tree of the given GP individual
     * @return the hash value of the desired GP tree of the given individual
     */
    default int hashOf(GPIndividual ind, int tree)
    {
        if(ind == null || ind.trees == null)
            throw new NullPointerException("The individual or its trees cannot be null");

        if(ind.trees.length != 1)
            throw new IllegalArgumentException("The method currently supports only individuals with 1 trees");

        return hashOfTree(ind.trees[tree].child);
    }
}
