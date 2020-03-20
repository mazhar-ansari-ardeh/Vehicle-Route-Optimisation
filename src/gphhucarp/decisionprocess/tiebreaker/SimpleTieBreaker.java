package gphhucarp.decisionprocess.tiebreaker;

import gphhucarp.core.Arc;
import gphhucarp.decisionprocess.TieBreaker;

/**
 * A simple tie breaker between two arcs uses the natural comparator.
 */

public class SimpleTieBreaker extends TieBreaker
{
    private static final SimpleTieBreaker cache = new SimpleTieBreaker();

    @Override
    public int breakTie(Arc arc1, Arc arc2)
    {
        return arc1.compareTo(arc2);
    }

    public SimpleTieBreaker()
    {}

    public static SimpleTieBreaker getInstance()
    {
        return cache;
    }
}
