package tl.gp.similarity;

import ec.gp.GPTree;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import tl.gp.niching.PhenoCharacterisation;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class CorrPhenoTreeSimilarityMetric implements TreeDistanceMetric<List<double[]>>
{

    private PhenoCharacterisation ch;

    /**
     * Constructs a new object with the given decision-making situations.
     * @param situations This constructor does not preserve a deep copy of the given list and hence, any modifications to the
     *                   list outside the scope of this class affect the behavior of this class.
     */
    public CorrPhenoTreeSimilarityMetric(List<ReactiveDecisionSituation> situations)
    {
        ch = new PhenoCharacterisation(situations);
    }

    @Override
    public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
    {
        List<double[]> ch1 = characterise(tree1);
        List<double[]> ch2 = characterise(tree2);

        SpearmansCorrelation sc = new SpearmansCorrelation();

        double sum = 0;
        for (int i = 0; i < ch1.size(); i++)
        {
            sum += sc.correlation(ch1.get(i), ch2.get(i));
        }

        return sum / ch1.size();
    }

    @Override
    public List<double[]> characterise(GPRoutingPolicy tree)
    {
        synchronized (cache)
        {
            int CACHE_SIZE = 1000;
            if (cache.size() > CACHE_SIZE)
                cache.clear();

            return cache.computeIfAbsent(tree.getGPTree(), t -> {
                List<int[]> ints = ch.cha(tree);
                List<double[]> doubles = new ArrayList<>();
                ints.forEach(ints1 -> doubles.add(copyFromIntArray(ints1)));

                return doubles;
            });
        }

//        List<int[]> ints = ch.cha(tree);
//        List<double[]> doubles = new ArrayList<>();
//        ints.forEach(ints1 -> doubles.add(copyFromIntArray(ints1)));
//
//        return doubles;
    }

    private final WeakHashMap<GPTree, List<double[]>> cache = new WeakHashMap<>();


    private static double[] copyFromIntArray(int[] source) {
        double[] dest = new double[source.length];
        for(int i=0; i<source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }
}
