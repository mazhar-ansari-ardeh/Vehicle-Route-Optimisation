package tl.knowledge.surrogate.lsh;

import tl.knowledge.sst.lsh.Vector;

import java.util.List;

public class FittedVector extends Vector
{
    private double fitness = Double.POSITIVE_INFINITY;

    public double getFitness()
    {
        return fitness;
    }

    public FittedVector(int dimensions)
    {
        super(dimensions);
    }

    public FittedVector(Vector other)
    {
        super(other);
        if(other instanceof FittedVector)
            this.fitness = ((FittedVector) other).fitness;
    }

    public FittedVector(String key, double[] values, double fitness)
    {
        super(key, values);
        this.fitness = fitness;
    }

    public FittedVector(int[] values, double fitness) {
        super(values);
        this.fitness = fitness;
    }

    public FittedVector(List<? extends Number> values, double fitness)
    {
        super(values);
        this.fitness = fitness;
    }
}
