package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;

import java.util.*;
import java.util.stream.Stream;

public class SurrogateFitness
{
    private PoolFilter filter;

    public void setFilter(PoolFilter filter)
    {
        this.filter = filter;
    }

    public void setRadius(double radius)
    {
//        this.radius = radius;
    }

    public LinkedList<Individual[]> getSurrogatePool()
    {
        return this.surrogatePool;
    }

    private GPRoutingPolicy[] knnPool = null;

    private LinkedList<Individual[]> surrogatePool = new LinkedList<>();

    private List<ReactiveDecisionSituation> situations;

    private PhenotypicTreeSimilarityMetric metric;

    public SurrogateFitness()
    {
    }

    public void setSituations(List<ReactiveDecisionSituation> situations)
    {
        this.situations = situations;
        metric = new PhenotypicTreeSimilarityMetric(situations);
    }

    public List<ReactiveDecisionSituation> getSituations()
    {
        return situations;
    }

    public SurrogateFitness(double radius, PoolFilter filter)
    {
//        this.radius = radius;
        this.filter = filter;
    }

//    private GPRoutingPolicy[] hartFilter(List<Individual[]> pops)
//    {
//        ArrayList<GPRoutingPolicy> retval = new ArrayList<>();
//        for(Individual[] pop : pops)
//        {
//            GPRoutingPolicy[] ret = hartFilter(pop);
//            retval.addAll(Arrays.asList(ret));
//        }
//
//        return retval.toArray(new GPRoutingPolicy[]{});
//    }
//
//    private GPRoutingPolicy[] hartFilter(Individual[] pop)
//    {
////            List<ReactiveDecisionSituation> situations = getAllSeenSituations().subList(0, 10);
//        PhenotypicTreeSimilarityMetric metric = new PhenotypicTreeSimilarityMetric(situations);
//
//        List<GPRoutingPolicy> pop2 = Stream.of(pop).sorted().map(p -> new GPRoutingPolicy(filter, ((GPIndividual)p).trees[0])).collect(Collectors.toList());
//        ArrayList<GPRoutingPolicy> R = new ArrayList<>();
//        R.add(pop2.get(0));
//        pop2.remove(0);
//        while(!pop2.isEmpty())
//        {
//            GPRoutingPolicy x = pop2.get(0);
//            pop2.remove(0);
//            double distance = metric.distance(R, x);
//            if(distance > radius)
//            {
//                R.add(x);
//            }
//        }
//
//        return R.toArray(new GPRoutingPolicy[0]);
//    }

    private GPRoutingPolicy[] unityFilter(Individual[] pop)
    {
        return Stream.of(pop).sorted().map(p -> new GPRoutingPolicy(filter, ((GPIndividual) p).trees[0])).toArray(GPRoutingPolicy[]::new);
    }

    public void updateSurrogatePool(Individual[] population)
    {
        surrogatePool.addFirst(population);
        if(surrogatePool.size() > 1)
            surrogatePool.removeLast();
        knnPool = null; // Do this so that the KNNFitness function will update it.
    }

    private GPRoutingPolicy[] unityFilter(LinkedList<Individual[]> pops)
    {
        ArrayList<GPRoutingPolicy> retval = new ArrayList<>();
        for(Individual[] pop : pops)
        {
            GPRoutingPolicy[] ret = unityFilter(pop);
            retval.addAll(Arrays.asList(ret));
        }

        return retval.toArray(new GPRoutingPolicy[]{});
    }

    public double fitness(Individual ind)
    {
        if(knnPool == null)
        {
            knnPool = unityFilter(surrogatePool);
        }

        double minDis = Double.POSITIVE_INFINITY;
        GPRoutingPolicy best = null;

        GPRoutingPolicy indPolicy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        indPolicy.setName("To find");

        for (GPRoutingPolicy p : knnPool)
        {
            double dist = metric.distance(indPolicy, p);
            if (dist < minDis)
            {
                minDis = dist;
                best = p;
            }
            if (dist == 0)
                break;
        }
        return best != null ? best.getGPTree().owner.fitness.fitness() : Double.MAX_VALUE;
    }
}



//interface SurrogatePoolUpdatePolicy
//{
//    LinkedList<Individual[]> update(LinkedList<Individual[]> pool, Individual[] pop);
//}
//
//class ReplacementUpdatePolicy implements SurrogatePoolUpdatePolicy
//{
//    private int gensToRetain = 1;
//
//    ReplacementUpdatePolicy(int gensToRetain)
//    {
//        this.gensToRetain = gensToRetain;
//    }
//
//    @Override
//    public LinkedList<Individual[]> update(LinkedList<Individual[]> pool, Individual[] pop)
//    {
//        pool.addFirst(pop);
//        if(pool.size() > gensToRetain)
//            pool.removeLast();
//
//        return pool;
//    }
//}
//
//class SimilarityUpdatePolicy implements SurrogatePoolUpdatePolicy
//{
//
//    @Override
//    public LinkedList<Individual[]> update(LinkedList<Individual[]> pool, Individual[] pop)
//    {
//        return null;
//    }
//}