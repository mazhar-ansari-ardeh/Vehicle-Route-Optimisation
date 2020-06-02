package tl.gp.similarity;

import ec.gp.GPTree;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.characterisation.TaskIndexCharacterisation;
import tl.gp.niching.PhenoCharacterisation;

import java.util.HashMap;
import java.util.List;

/**
 * Implements the distance metric based on the Euclidean phenotypic distance of the trees.
 * This class uses the {@link TaskIndexCharacterisation} for characterising GP rules.
 */
public class PhenotypicTreeSimilarityMetric implements SituationBasedTreeSimilarityMetric
{
    TaskIndexCharacterisation ch;

    public PhenotypicTreeSimilarityMetric()
    {
    }

    /**
     * Calculates the phenotypic distance of two trees.
     * @param tree1 a GP tree. This argument cannot be {@code null}.
     * @param tree2 another GP tree. This argument cannot be {@code null}.
     * @return the Euclidean distance
     */
    @Override
    public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
    {
//        tree1.getGPTree();
        int[] ch1 = characterise(tree1);
        int[] ch2 = characterise(tree2);

        double distance = PhenoCharacterisation.distance(ch1, ch2);
        return distance;
    }

    private final HashMap<GPTree, int[]> cache = new HashMap<>();

    public int[] characterise(GPRoutingPolicy tree)
    {
        synchronized (cache)
        {
            int CACHE_SIZE = 10000;
            if(cache.size() > CACHE_SIZE)
                cache.clear();
            return cache.computeIfAbsent(tree.getGPTree(), t -> ch.characterise(tree));
//            return cache.computeIfAbsent(tree.getGPTree().treeHashCode(), t -> ch.characterise(tree));
        }
    }

    @Override
    public void setSituations(List<ReactiveDecisionSituation> situations)
    {
        ch = new TaskIndexCharacterisation(situations);
        synchronized (cache)
        {
            cache.clear();
        }
    }

    @Override
    public String getName() {
        return "Phenotypic";
    }

    //    public double distance(ArrayList<GPRoutingPolicy> policies, GPRoutingPolicy aPolicy)
//    {
//        int[] ch1 = ch.characterise(policies);
//        int[] ch2 = ch.characterise(aPolicy);
//        double distance = PhenoCharacterisation.distance(ch1, ch2);
//        return distance;
//    }
}
