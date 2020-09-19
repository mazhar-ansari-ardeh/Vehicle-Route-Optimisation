package tl.knowledge.surrogate;

import ec.Individual;

public interface Surrogate
{
    void updateSurrogatePool(Individual[] population, String source);

    double fitness(Individual ind);
}
