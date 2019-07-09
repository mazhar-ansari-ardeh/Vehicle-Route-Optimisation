package tl.gp.simplification;

import ec.EvolutionState;
import ec.gp.GPIndividual;

public class TreeSimplifier
{
    protected TreeSimplifier next = null;

    // Provided a basic implementation so that the class can be concrete and therefore, used to create variables
    // without needing to use a different class to do it.
    protected boolean simplify(EvolutionState state, GPIndividual ind)
    {
        return false;
    }

    public TreeSimplifier setNext(TreeSimplifier nextSimplifier)
    {
        // This implementation does not check for replications in the chain so that multiple simplifiers can be chained
        // together.
        TreeSimplifier lastOne = this;
        while (lastOne.next != null)
            lastOne = lastOne.next;

        lastOne.next = nextSimplifier;

        return this;
    }

    public boolean simplifyTree(EvolutionState state, GPIndividual ind)
    {
        boolean retval = simplify(state, ind);
        if (next != null)
            retval |= next.simplifyTree(state, ind);

        return retval;
    }
}
