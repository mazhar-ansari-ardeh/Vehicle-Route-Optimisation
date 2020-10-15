package tl.knowledge.surrogate.lsh;

import ec.Individual;
import ec.gp.GPIndividual;
import ec.util.MersenneTwisterFast;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.knowledge.sst.lsh.LSH;
import tl.knowledge.sst.lsh.Vector;
import tl.knowledge.sst.lsh.families.EuclidianHashFamily;
import tl.knowledge.sst.lsh.families.HashFamily;
import tl.knowledge.surrogate.Surrogate;

import java.util.List;

public class LSHSurrogate implements Surrogate
{
    LSH lsh;

    PoolFilter filter;

    TaskIndexCharacterisation trc;

    int neighboursSize;

    public LSHSurrogate(int dimensions, int radius, int numberOfHashes, int numberOfHashTables,
                        MersenneTwisterFast rand, PoolFilter filter, List<ReactiveDecisionSituation> situations,
                        int neighboursSize)
    {
        final int W = 10;
        int w = W * radius;
        w = w == 0 ? 1 : w;
        HashFamily family = new EuclidianHashFamily(w, dimensions);
        lsh = new LSH(family);
        lsh.buildIndex(numberOfHashes, numberOfHashTables, rand);

        this.filter = filter;

        trc = new TaskIndexCharacterisation(situations.subList(0, Math.min(dimensions, situations.size())));
        this.neighboursSize = neighboursSize;
    }


    @Override
    public void updateSurrogatePool(Individual[] population, String source)
    {
        for(int i = 0; i < population.length; i++)
        {
            GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)population[i]).trees[0]);
            int[] characterise = trc.characterise(policy);

            double fitness = population[i].fitness.fitness();
            lsh.add(new FittedVector(characterise, fitness));
        }
    }

    public double fitness(GPRoutingPolicy policy)
    {
        int[] characterise = trc.characterise(policy);

        double retval = 0;
        List<Vector> query = lsh.queryNearest(new Vector(characterise));
        if(query.size() == 0)
            throw new RuntimeException("The query did not return anything.");
        for (Vector vector : query)
        {
            retval += ((FittedVector) vector).getFitness();
        }

        return retval / query.size();
    }

    @Override
    public double fitness(Individual ind)
    {
        GPRoutingPolicy policy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        return fitness(policy);
    }
}
