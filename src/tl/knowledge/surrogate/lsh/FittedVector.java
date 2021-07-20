package tl.knowledge.surrogate.lsh;

import tl.knowledge.sst.lsh.Vector;

import java.util.List;

/**
 * An implementation of the {@link Vector} class that stores the fitness of individual it is representing, as well as
 * the generation it is from.
 */
public class FittedVector extends Vector
{
    private double fitness = Double.POSITIVE_INFINITY;

    public double getFitness()
    {
        return fitness;
    }

    private int generation;

    public int getGeneration()
    {
        return generation;
    }

    public FittedVector(int dimensions)
    {
        super(dimensions);
    }

    public FittedVector(Vector other)
    {
        super(other);
        if(other instanceof FittedVector)
        {
            this.fitness = ((FittedVector) other).fitness;
            this.generation = ((FittedVector) other).generation;
        }
    }

    public FittedVector(String key, double[] values, double fitness, int generation)
    {
        super(key, values);
        this.fitness = fitness;
        this.generation = generation;
    }

    public FittedVector(int[] values, double fitness, int generation) {
        super(values);
        this.fitness = fitness;
        this.generation = generation;
    }

    public FittedVector(List<? extends Number> values, double fitness, int generation)
    {
        super(values);
        this.fitness = fitness;
        this.generation = generation;
    }
}
