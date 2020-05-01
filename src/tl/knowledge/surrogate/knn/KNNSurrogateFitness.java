package tl.knowledge.surrogate.knn;

import ec.Individual;
import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KNNSurrogateFitness
{
    private PoolFilter filter;

    private KNNPool knnPool = new KNNPool();

    private List<ReactiveDecisionSituation> situations;

    public void setMetric(SituationBasedTreeSimilarityMetric metric)
    {
        this.metric = metric;
    }

    private SituationBasedTreeSimilarityMetric metric;

    public void rerevaluate(Function<Individual, Double> with)
    {
        knnPool.reevaluate(with);
    }

    public KNNSurrogateFitness(SituationBasedTreeSimilarityMetric metric)
    {
        this.metric = metric;
    }

    public KNNSurrogateFitness()
    {

    }

    public String logSurrogatePool()
    {
        return knnPool.poolLog();
    }

    public void updateSurrogatePool(Individual[] population, String source)
    {
        knnPool.update(population, source, filter, metric, situations);
    }

    public List<GPIndividual> getSurrogatePool()
    {
        return knnPool.pool.stream().sorted(Comparator.comparingDouble(p -> p.fitness))
                                    .map(p -> p.policy.getGPTree().owner)

                .collect(Collectors.toList());
    }

    public boolean isKNNPoolEmpty()
    {
        return knnPool.pool.isEmpty();
    }

    public double fitness(Individual ind)
    {
        double minDis = Double.POSITIVE_INFINITY;
        KNNPoolItem best = null;

        GPRoutingPolicy indPolicy = new GPRoutingPolicy(filter, ((GPIndividual)ind).trees[0]);
        indPolicy.setName("To find");

        for (KNNPoolItem p : knnPool)
        {
            double dist = metric.distance(indPolicy, p.policy);
            if (dist < minDis)
            {
                minDis = dist;
                best = p;
            }
            if (dist == 0)
                break;
        }
        return best != null ? best.fitness : Double.MAX_VALUE;
    }

    public void setFilter(PoolFilter filter)
    {
        this.filter = filter;
        knnPool.forEach(p -> p.policy.setPoolFilter(filter));
    }

    public void setSituations(List<ReactiveDecisionSituation> situations)
    {
        if(situations == null || situations.size() == 0)
            throw new RuntimeException("Situations cannot be null or empty");

        this.situations = situations;
        metric.setSituations(situations);
    }

    public List<ReactiveDecisionSituation> getSituations()
    {
        return situations;
    }

    public void setSurrogateUpdatePolicy(KNNPoolUpdatePolicy policy)
    {
        knnPool.setUpdatePolicy(policy);
    }
}
