package tl.knowledge.surrogate.knn.kmeans;
import tl.knowledge.surrogate.knn.FeatureVector;

import java.util.Arrays;
import java.util.Objects;

public class Record implements FeatureVector
{
    private final String description;
    private final double[] features;

    public Record(String description, double[] features)
    {
        this.description = description;
        this.features = features;
    }

    public Record(int[] features)
    {
        this.description = null;
        this.features = new double[features.length];
        for (int i = 0; i < features.length; i++)
        {
            this.features[i] = (double) features[i];
        }
    }

    public String getDescription()
    {
        return description;
    }

    public double[] getFeatures()
    {
        return features;
    }

    @Override
    public String toString() {
        String prefix = description == null || description
                .trim()
                .isEmpty() ? "Record" : description;

        return prefix + ": " + Arrays.toString(features);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Record record = (Record) o;
        return Objects.equals(getDescription(), record.getDescription()) && Arrays.equals(getFeatures(), record.getFeatures());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getFeatures());
    }
}