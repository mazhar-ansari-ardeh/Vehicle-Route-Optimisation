package tl.gp.niching;

import ec.Individual;
import ec.gp.GPIndividual;
import tl.gp.PopulationUtils;

import java.util.ArrayList;

public class SimpleNichingAlgorithm implements NicheAlgorithm
{
    /**
     * The niche radius. A negative value for this field will effectively disable the niching operation.
     */
    private final double radius;

    /**
     * The capacity of each niche, that is, the number of individuals inside a niche.
     */
    private final int capacity;

    /**
     * Creates a new instance of the algorithm.
     * @param radius the niche radius. A negative value for this parameter will effectively disable the niching operation.
     * @param capacity the capacity of each niche. The value of this parameter must be a positive integer.
     */
    public SimpleNichingAlgorithm(double radius, int capacity)
    {
        if(capacity <= 0)
            throw new IllegalArgumentException("Niche capacity must be positive integer: " + capacity);

        this.radius = radius;
        this.capacity = capacity;
    }

    private static int compare(GPIndividual i1, GPIndividual i2)
    {
        if (i1.fitness.fitness() < i2.fitness.fitness())
            return -1;
        else if (i1.fitness.fitness() > i2.fitness.fitness())
            return 1;
        else
            return Integer.compare(i1.trees[0].child.depth(), i2.trees[0].child.depth());
    }

//    @Override
//    public Individual[] applyNiche(Individual[] population)
//    {
//        if(population == null)
//            throw new IllegalArgumentException("The population to perform niching on cannot be null.");
//
//        return new Individual[0];
//    }

    /**
     * Applies a simple niching algorithm on the given population and returns a new array that contains the individuals that
     * survived the niching.
     *
     * @param pop the population to apply niching on. The function will sort this array based on the fitness of individuals.
     */
    public GPIndividual[] applyNiche(GPIndividual[] pop)
    {
        if(pop == null || pop.length == 0)
            throw new IllegalArgumentException("Population cannot be null or empty.");

        ArrayList<GPIndividual> retval = new ArrayList<>();
        PopulationUtils.sort(pop);
        ArrayList<GPIndividual> niche = new ArrayList<>();
        double nicheCenter = pop[0].fitness.fitness();
        niche.add(pop[0]);
        for (int i = 1; i < pop.length; i++)
        {
            GPIndividual individual = pop[i];
            if (Math.abs(nicheCenter - individual.fitness.fitness()) <= radius)
            {
                niche.add(individual);
            }
            else
            {
                // Found a new niche
                nicheCenter = individual.fitness.fitness();
//                niche.sort(Comparator.comparingInt(ind -> ind.trees[0].child.depth()));
                niche.sort(SimpleNichingAlgorithm::compare);
                retval.addAll(niche.subList(0, niche.size() >= capacity ? capacity : niche.size()));
                niche.clear();
                niche.add(individual);
            }
        }

        niche.sort(SimpleNichingAlgorithm::compare);
        retval.addAll(niche.subList(0, niche.size() >= capacity ? capacity : niche.size()));
        return retval.toArray(new GPIndividual[0]);
    }
}
