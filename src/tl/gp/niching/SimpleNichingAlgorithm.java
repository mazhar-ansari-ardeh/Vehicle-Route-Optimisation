package tl.gp.niching;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.multiobjective.MultiObjectiveFitness;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.PopulationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            // If the individual has been cleared in the source domain, then ignore it.
            double indFitness = individual.fitness.fitness();
            if(indFitness== Double.POSITIVE_INFINITY || indFitness == Double.NEGATIVE_INFINITY )
                continue;
            if (Math.abs(nicheCenter - indFitness) <= radius)
            {
                niche.add(individual);
            }
            else
            {
                // Found a new niche
                nicheCenter = indFitness;
//              niche.sort(Comparator.comparingInt(ind -> ind.trees[0].child.depth()));
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

    private static boolean isCleared(MultiObjectiveFitness fitness)
    {
        for (int i = 0; i < fitness.objectives.length; ++i) {
            if (fitness.maximize[i]) {
                if(fitness.objectives[i] != Double.NEGATIVE_INFINITY)
                    return false;
            }
            else {
                if(fitness.objectives[i] != Double.POSITIVE_INFINITY)
                    return false;
            }
        }

        return true;
    }

    private static void clear(MultiObjectiveFitness fitness) {
        for (int i = 0; i < fitness.objectives.length; ++i) {
            if (fitness.maximize[i]) {
                fitness.objectives[i] = Double.NEGATIVE_INFINITY;
            }
            else {
                fitness.objectives[i] = Double.POSITIVE_INFINITY;
            }
        }
        // this.cleared = true;
    }

    public static void clearPopulation(final EvolutionState state, List<ReactiveDecisionSituation> dps, final double radius, final int capacity) {
        for (final Subpopulation subpop : state.population.subpops) {
            final Individual[] sortedPop = subpop.individuals;
            Arrays.sort(sortedPop);
            final PhenoCharacterisation pc = new PhenoCharacterisation(dps, new GPRoutingPolicy(((GPIndividual)sortedPop[0]).trees[0]));
            final List<int[]> sortedPopCharLists = new ArrayList<>();
            for (final Individual indi : sortedPop)
            {
                final int[] charList = pc.characterise(new GPRoutingPolicy(((GPIndividual)indi).trees[0]));
                sortedPopCharLists.add(charList);
            }
            for (int i = 0; i < sortedPop.length; ++i)
            {
                if (isCleared((MultiObjectiveFitness) sortedPop[i].fitness))
                    continue;
                int numWinners = 1;
                for (int j = i + 1; j < sortedPop.length; ++j)
                {
                    if (isCleared((MultiObjectiveFitness) sortedPop[j].fitness))
                        continue;

                    final double distance = PhenoCharacterisation.distance(sortedPopCharLists.get(i), sortedPopCharLists.get(j));
                    if (distance <= radius)
                    {
                        if (numWinners < capacity)
                        {
                            ++numWinners;
                        }
                        else {
                            clear((MultiObjectiveFitness) sortedPop[j].fitness);
//                            state.output.warning("Cleared: " + ((GPIndividual)sortedPop[i]).trees[0].child.makeLispTree());
                        }
                    }
                }
            }
        }
    }
}
