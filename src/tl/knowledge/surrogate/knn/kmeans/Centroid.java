package tl.knowledge.surrogate.knn.kmeans;

import tl.knowledge.surrogate.knn.FeatureVector;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates all coordinates for a particular cluster centroid.
 */
public class Centroid implements FeatureVector
{

    /**
     * The centroid coordinates.
     */
    private final double[] coordinates;

    public Centroid(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Centroid centroid = (Centroid) o;
        return Arrays.equals(getCoordinates(), centroid.getCoordinates());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getCoordinates());
    }

    @Override
    public String toString()
    {
        int iMax = coordinates.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(Math.round(coordinates[i]));
            if (i == iMax)
                return "Centroid: " + b.append(']').toString();
            b.append(", ");
        }
    }

    @Override
    public double[] getFeatures()
    {
        return coordinates;
    }
}