package tl.gp.similarity;

import ec.gp.GPTree;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.niching.PhenoCharacterisation;

import java.util.List;
import java.util.WeakHashMap;

/**
 * Implements the distance metric based on the phenotypic distance of the trees.
 */
public class PhenotypicTreeSimilarityMetric implements TreeDistanceMetric
{
    /**
     * The decision making situations that will be used to observe the phenotypic characteristics of routing policies.
     */
//    private final List<ReactiveDecisionSituation> situations;
    PhenoCharacterisation ch;

    /**
     * Constructs a new object with the given decision-making situations.
     * @param situations This constructor does not preserve a deep copy of the given list and hence, any modifications to the
     *                   list outside the scope of this class affect the behavior of this class.
     */
    public PhenotypicTreeSimilarityMetric(List<ReactiveDecisionSituation> situations)
    {
        ch = new PhenoCharacterisation(situations);
    }

    /**
     * Calculates the phenotypic distance of two trees.
     * @param tree1 a GP tree. This argument cannot be {@code null}.
     * @param tree2 another GP tree. This argument cannot be {@code null}.
     * @return
     */
    @Override
    public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
    {
        tree1.getGPTree();
        int[] ch1 = getPhenChars(tree1);
        int[] ch2 = getPhenChars(tree2);

        double distance = PhenoCharacterisation.distance(ch1, ch2);
        return distance;
    }

    private final WeakHashMap<GPTree, int[]> cache = new WeakHashMap<>();
    private int[] getPhenChars(GPRoutingPolicy tree)
    {
        synchronized (cache)
        {
            return cache.computeIfAbsent(tree.getGPTree(), t -> ch.characterise(tree));
        }
    }
}
