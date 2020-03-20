package tl.knowledge.surrogate.knn.kmeans;

import tl.knowledge.surrogate.knn.FeatureVector;

public class EuclideanDistance implements Distance
{

//    @Override
    private double calculate(double[] f1, double[] f2) {
        double sum = 0;
        for (int i = 0; i < f1.length; i++)
        {
            sum += Math.pow(f1[i] - f2[i], 2);
        }

        return Math.sqrt(sum);
    }

    @Override
    public double calculate(FeatureVector f1, FeatureVector f2)
    {
        return calculate(f1.getFeatures(), f2.getFeatures());
    }
}