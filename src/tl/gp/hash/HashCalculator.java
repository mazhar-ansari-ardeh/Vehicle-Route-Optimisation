package tl.gp.hash;

import ec.gp.GPIndividual;
import ec.gp.GPNode;

public interface HashCalculator
{
    int hashOfTree(GPNode tree);

    default int hashOf(GPIndividual ind)
    {
        if(ind == null || ind.trees == null)
            throw new NullPointerException("The individual or its trees cannot be null");

        if(ind.trees.length != 1)
            throw new IllegalArgumentException("The method currently supports only individuals with 1 trees");

        return hashOfTree(ind.trees[0].child);
    }
}
