package tl.gp.similarity;

import ec.gp.GPTree;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import tl.gp.characterisation.TaskRankCharacterisation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CorrPhenoTreeSimilarityMetric implements SituationBasedTreeSimilarityMetric
{

    private TaskRankCharacterisation ch;


    public CorrPhenoTreeSimilarityMetric() {
    }

    private double calcDistance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
    {
        List<double[]> ch1 = characterise(tree1);
        List<double[]> ch2 = characterise(tree2);

        SpearmansCorrelation sc = new SpearmansCorrelation();

        double sum = 0;
        for (int i = 0; i < ch1.size(); i++)
        {
            if(ch1.get(i).length < 2 || ch2.get(i).length < 2)
                continue;
            sum += sc.correlation(ch1.get(i), ch2.get(i));
        }

        return 1 - (sum / ch1.size());
    }

    private final int CACHE_SIZE = 5000;

    @Override
    public double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2)
    {
        synchronized (corrCache)
        {
            if (cache.size() > CACHE_SIZE)
                corrCache.clear();
            return corrCache.computeIfAbsent(new ImmutablePair<>(tree1.getGPTree(), tree2.getGPTree()), gpTreeGPTreePair ->
                            calcDistance(tree1, tree2)
                    );
        }
    }

//    @Override
    public List<double[]> characterise(GPRoutingPolicy tree)
    {
        synchronized (cache)
        {
            if (cache.size() > CACHE_SIZE)
                cache.clear();

            return cache.computeIfAbsent(tree.getGPTree(), t -> {
                List<int[]> ints = ch.characterise(tree);
                List<double[]> doubles = new ArrayList<>();
                ints.forEach(ints1 -> doubles.add(copyFromIntArray(ints1)));

                return doubles;
            });
        }
    }

    private final HashMap<GPTree, List<double[]>> cache = new HashMap<>();

    private final HashMap<Pair<GPTree, GPTree>, Double> corrCache = new HashMap<>();

    private static double[] copyFromIntArray(int[] source) {
        double[] dest = new double[source.length];
        for(int i=0; i<source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }

    @Override
    public void setSituations(List<ReactiveDecisionSituation> situations)
    {
        ch = new TaskRankCharacterisation(situations);
        synchronized (cache)
        {
            cache.clear();
            corrCache.clear();
        }
    }

    @Override
    public String getName() {
        return "CorrPhenotypic";
    }
}
