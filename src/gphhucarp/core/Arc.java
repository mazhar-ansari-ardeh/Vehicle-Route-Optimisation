package gphhucarp.core;

import org.apache.commons.math3.random.RandomDataGenerator;
import util.random.AbstractRealSampler;
import util.random.NormalSampler;

import java.io.Serializable;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * An arc, which is a directed edge of the graph.
 * It has
 *  - (from, to) nodes,
 *  - serving cost,
 *  - inverser arc,
 *  - random demand (represented by a demand sampler)
 *  - random deadheading cost (represented by a cost sampler)
 *
 * Natural comparison: (a1, b1) < (a2, b2) if a1 < a2 or a1 == a2 and b1 < b2.
 *
 * In addition, it has two fields for decision making process
 *  - remaining demand fraction
 *  - priority
 *
 * Created by gphhucarp on 14/06/17.
 */

public class Arc implements Comparable<Arc>, Serializable
{
    private final int from; // from node id
    private final int to; // to node id
    private final double serveCost; // serve cost >= 0

    private Arc inverse; // the inverse arc is (to, from)

    // In UCARP, the demand and costs are random variables.
    // Their distributions are known in advance (e.g. estimated from history).
    private final AbstractRealSampler demandSampler; // the sampler for the demand.
    private final AbstractRealSampler costSampler; // the sampler for the deadheading cost.

    private double priority; // the priority for decision making process.

    public Arc(int from, int to, double demand,
               double serveCost, double deadheadingCost,
               Arc inverse, double demandUncertaintyLevel, double costUncertaintyLevel) {
        this.from = from;
        this.to = to;
        this.serveCost = serveCost;
        this.inverse = inverse;

        this.demandSampler = NormalSampler.create(demand, demandUncertaintyLevel * demand);
        this.costSampler = NormalSampler.create(deadheadingCost, costUncertaintyLevel * deadheadingCost);
    }

    private Arc(int from, int to, double serveCost, AbstractRealSampler demandSampler, AbstractRealSampler costSampler)
    {
        this.from = from;
        this.to = to;
        this.serveCost = serveCost;
        this.demandSampler = demandSampler;
        this.costSampler = costSampler;
    }

    public static Arc copy(Arc other)
    {
        return new Arc(other);
////        return new Arc(other);
//        int hash = other.hashCode();
//        synchronized (CACHE)
//        {
//            return CACHE.computeIfAbsent(hash, i -> new Arc(other));
//        }
    }


//    private static final WeakHashMap<Integer, Arc> CACHE = new WeakHashMap<>();
//    public static int cacheSize()
//    {
//        synchronized (CACHE)
//        {
//            return CACHE.size();
//        }
//    }

    public Arc(Arc arc)
    {
        this.from = arc.from;
        this.to = arc.to;
        this.serveCost = arc.serveCost;
        this.demandSampler = (AbstractRealSampler) arc.demandSampler.clone();
        this.costSampler = (AbstractRealSampler) arc.costSampler.clone();
        this.priority = arc.priority;

        if(arc.inverse != null)
        {
            AbstractRealSampler cDemandSampler = (AbstractRealSampler) arc.inverse.demandSampler.clone();
            AbstractRealSampler cCostSampler = (AbstractRealSampler) arc.inverse.costSampler.clone();
            this.inverse = new Arc(this.from, this.to, this.serveCost, cDemandSampler, cCostSampler);
            this.inverse.priority = arc.inverse.priority;

            this.inverse.inverse = this;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Arc arc = (Arc) o;

        if (from != arc.from) return false;
        if (to != arc.to) return false;
        if (Double.compare(arc.serveCost, serveCost) != 0) return false;
        if (Double.compare(arc.priority, priority) != 0) return false;
//        if (inverse != null ? !inverse.equals(arc.inverse) : arc.inverse != null) return false;
        if (!demandSampler.equals(arc.demandSampler)) return false;
        return costSampler.equals(arc.costSampler);

    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = from;
        result = 31 * result + to;
        temp = Double.doubleToLongBits(serveCost);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + demandSampler.hashCode();
        result = 31 * result + costSampler.hashCode();
//        result = 31 * result + (inverse != null ? inverse.hashCode() : 0);
//        temp = Double.doubleToLongBits(priority);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public double getServeCost() {
        return serveCost;
    }

    public Arc getInverse() {
        return inverse;
    }

    public double getExpectedDemand() {
        return demandSampler.getMean();
    }

    public double getExpectedDeadheadingCost() {
        return costSampler.getMean();
    }

    public double getPriority() {
        return priority;
    }

//    public void setServeCost(double serveCost) {
//        this.serveCost = serveCost;
//    }

    public void setInverse(Arc inverse) {
        this.inverse = inverse;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    /**
     * Whether the arc is a task, i.e. is required to be served.
     * @return true if the arc is a task, false otherwise.
     */
    public boolean isTask() {
        return demandSampler.getMean() > 0;
    }

    /**
     * Sample an actual demand based on the sampler and a random data generator.
     * If the task is undirected, then set its inverse as well.
     * @param rdg the random data generator.
     * @return the sampled demand.
     */
    public double sampleDemand(RandomDataGenerator rdg) {
        double sampledDemand = demandSampler.next(rdg);

        if (sampledDemand < 0)
            sampledDemand = 0;

        return sampledDemand;
    }

    /**
     * Sample an actual deadheading cost based on the sampler and a random data generator.
     * @param rdg the random data generator.
     * @return the sampled deadheading cost.
     */
    public double sampleDeadheadingCost(RandomDataGenerator rdg) {
        double sampledDeadheadingCost = costSampler.next(rdg);

        // set the deadheading cost to infinity (the arc becomes temporarily unavailable
        // if the sampled value is negative.
        if (sampledDeadheadingCost < 0)
            sampledDeadheadingCost = Double.POSITIVE_INFINITY;

        return sampledDeadheadingCost;
    }

    /**
     * Whether this arc is prior to another arc.
     * An arc is prior to another arc if
     *   (1) it has a smaller priority value, or
     *   (2) they have the same priority, and this arc is better than the other arc.
     * @param o the other arc.
     * @return true if this arc is prior to the other, and false otherwise.
     */
    public boolean priorTo(Arc o) {
        if (Double.compare(priority, o.priority) < 0)
            return true;

        if (Double.compare(priority, o.priority) > 0)
            return false;

        return compareTo(o) < 0;
    }

    @Override
    public String toString() {
        return "(" + from + ", " + to + "), dem = " + demandSampler.getMean()
                + ", sc = " + serveCost + ", dc = " + costSampler.getMean() + " \n";
    }

    public String toSimpleString() {
        return "(" + from + ", " + to + ")";
    }

    @Override
    public int compareTo(Arc o) {
        if (from < o.from)
            return -1;

        if (from > o.from)
            return 1;

        if (to < o.to)
            return -1;

        if (to > o.to)
            return 1;

        return 0;
    }
}
