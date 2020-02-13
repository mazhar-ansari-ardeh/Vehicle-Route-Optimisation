package gphhucarp.decisionprocess;

import gphhucarp.core.Arc;

/**
 * A tie breaker breaks the tie between two arcs when they have the same priority.
 */

public abstract class TieBreaker implements Cloneable {

    public abstract int breakTie(Arc arc1, Arc arc2);

    @Override
    public Object clone()
    {
        try
        {
            return super.clone();
        } catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e); // This should never happen.
        }
    }
}
