package tl.knowledge.surrogate.knn.kmeans;

import ec.util.MersenneTwisterFast;
import tl.knowledge.surrogate.knn.FeatureVector;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Encapsulates an implementation of KMeans clustering algorithm.
 *
 * @author Ali Dehghani
 */
public class KMeans {

    private KMeans()
    {
        throw new IllegalAccessError("You shouldn't call this constructor");
    }

    /**
     * Performs the K-Means clustering algorithm on the given dataset.
     *
     * @param records       The dataset.
     * @param k             Number of Clusters.
     * @param distance      To calculate the distance between two items.
     * @param maxIterations Upper bound for the number of iterations.
     * @param rand          Will be used to generate random numbers.
     * @return K clusters along with their features.
     */
    public static Map<Centroid, List<FeatureVector>> fit(List<? extends FeatureVector> records, int k, Distance distance,
                                                         int maxIterations, MersenneTwisterFast rand)
    {
        applyPreconditions(records, k, distance, maxIterations);

        List<Centroid> centroids = randomCentroids(records, k, rand);
        Map<Centroid, List<FeatureVector>> clusters = new HashMap<>();
        Map<Centroid, List<FeatureVector>> lastState = new HashMap<>();

        // iterate for a pre-defined number of times
        for (int i = 0; i < maxIterations; i++) {
            boolean isLastIteration = i == maxIterations - 1;

            // in each iteration we should find the nearest centroid for each record
            for (FeatureVector record : records) {
                Centroid centroid = nearestCentroid(record, centroids, distance);
                assignToCluster(clusters, record, centroid);
            }

            // if the assignment does not change, then the algorithm terminates
            boolean shouldTerminate = isLastIteration || clusters.equals(lastState);
            lastState = clusters;
            if (shouldTerminate) {
                break;
            }

            // at the end of each iteration we should relocate the centroids
            centroids = relocateCentroids(clusters);
            clusters = new HashMap<>();
        }

        return lastState;
    }

    /**
     * Move all cluster centroids to the average of all assigned features.
     *
     * @param clusters The current cluster configuration.
     * @return Collection of new and relocated centroids.
     */
    private static List<Centroid> relocateCentroids(Map<Centroid, List<FeatureVector>> clusters) {
        return clusters
                .entrySet()
                .stream()
                .map(e -> average(e.getKey(), e.getValue()))
                .collect(toList());
    }

    /**
     * Moves the given centroid to the average position of all assigned features. If
     * the centroid has no feature in its cluster, then there would be no need for a
     * relocation. Otherwise, for each entry we calculate the average of all records
     * first by summing all the entries and then dividing the final summation value by
     * the number of records.
     *
     * @param centroid The centroid to move.
     * @param records  The assigned features.
     * @return The moved centroid.
     */
    private static Centroid average(Centroid centroid, List<FeatureVector> records) {
        // if this cluster is empty, then we shouldn't move the centroid
        if (records == null || records.isEmpty()) {
            return centroid;
        }

        // Since some records don't have all possible attributes, we initialize
        // average coordinates equal to current centroid coordinates
        double[] average = new double[centroid.getCoordinates().length];

        for (FeatureVector record : records)
        {
            double[] features = record.getFeatures();
            for (int i = 0; i < average.length; i++)
            {
                average[i] += features[i];
            }
        }

        for (int i = 0; i < average.length; i++)
        {
            average[i] = average[i] / records.size();
        }

        return new Centroid(average);
    }

    /**
     * Assigns a feature vector to the given centroid. If this is the first assignment for this centroid,
     * first we should create the list.
     *
     * @param clusters The current cluster configuration.
     * @param record   The feature vector.
     * @param centroid The centroid.
     */
    public static void assignToCluster(Map<Centroid, List<FeatureVector>> clusters, FeatureVector record, Centroid centroid) {
        clusters.compute(centroid, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(record);
            return list;
        });
    }

    /**
     * With the help of the given distance calculator, iterates through centroids and finds the
     * nearest one to the given record.
     *
     * @param record    The feature vector to find a centroid for.
     * @param centroids Collection of all centroids.
     * @param distance  To calculate the distance between two items.
     * @return The nearest centroid to the given feature vector.
     */
    private static Centroid nearestCentroid(FeatureVector record, List<Centroid> centroids, Distance distance) {
        double minimumDistance = Double.MAX_VALUE;
        Centroid nearest = null;

        for (Centroid centroid : centroids) {
            double currentDistance = distance.calculate(record, centroid);

            if (currentDistance < minimumDistance) {
                minimumDistance = currentDistance;
                nearest = centroid;
            }
        }

        return nearest;
    }

    public static Centroid nearestCentroid(FeatureVector record, Set<Centroid> centroids, Distance distance) {
        double minimumDistance = Double.MAX_VALUE;
        Centroid nearest = null;

        for (Centroid centroid : centroids) {
            double currentDistance = distance.calculate(record, centroid);

            if (currentDistance < minimumDistance) {
                minimumDistance = currentDistance;
                nearest = centroid;
            }
        }

        return nearest;
    }

    /**
     * Generates k random centroids. Before kicking-off the centroid generation process,
     * first we calculate the possible value range for each attribute. Then when
     * we're going to generate the centroids, we generate random coordinates in
     * the [min, max] range for each attribute.
     *
     * @param records The dataset which helps to calculate the [min, max] range for
     *                each attribute.
     * @param k       Number of clusters.
     * @return Collections of randomly generated centroids.
     */
    private static List<Centroid> randomCentroids(List<? extends FeatureVector> records, int k, MersenneTwisterFast random) {
        List<Centroid> centroids = new ArrayList<>();

        int featureLen = records.get(0).getFeatures().length;

        double[] maxs = new double[featureLen];
        Arrays.fill(maxs, Double.NEGATIVE_INFINITY);
        double[] mins = new double[featureLen];
        Arrays.fill(mins, Double.POSITIVE_INFINITY);

        for (FeatureVector record : records)
        {
            double[] features = record.getFeatures();
            for (int i = 0; i < featureLen; i++)
            {
                if(features[i] < mins[i])
                    mins[i] = features[i];
                if(features[i] > maxs[i])
                    maxs[i] = features[i];
            }
        }

        for (int i = 0; i < k; i++) {
//            Map<String, Double> coordinates = new HashMap<>();
            double[] coordinates = new double[featureLen];
            for (int j = 0; j < featureLen; j++)
            {
                coordinates[j] = random.nextDouble() * (maxs[j] - mins[j]) + mins[j];
            }
            centroids.add(new Centroid(coordinates));
        }

        return centroids;
    }

    private static void applyPreconditions(List<? extends FeatureVector> records, int k, Distance distance, int maxIterations) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("The dataset can't be empty");
        }

        if (k <= 1) {
            throw new IllegalArgumentException("It doesn't make sense to have less than or equal to 1 cluster");
        }

        if (distance == null) {
            throw new IllegalArgumentException("The distance calculator is required");
        }

        if (maxIterations <= 0) {
            throw new IllegalArgumentException("Max iterations should be a positive number");
        }
    }
}