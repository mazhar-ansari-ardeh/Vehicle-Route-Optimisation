package tl.knowledge.surrogate.knn.dbscan;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.MathUtils;

/**
 * DBSCAN (density-based spatial clustering of applications with noise) algorithm.
 * <p>
 * The DBSCAN algorithm forms clusters based on the idea of density connectivity, i.e.
 * a point p is density connected to another point q, if there exists a chain of
 * points p<sub>i</sub>, with i = 1 .. n and p<sub>1</sub> = p and p<sub>n</sub> = q,
 * such that each pair &lt;p<sub>i</sub>, p<sub>i+1</sub>&gt; is directly density-reachable.
 * A point q is directly density-reachable from point p if it is in the &epsilon;-neighborhood
 * of this point.
 * <p>
 * Any point that is not density-reachable from a formed cluster is treated as noise, and
 * will thus not be present in the result.
 * <p>
 * The algorithm requires two parameters:
 * <ul>
 *   <li>eps: the distance that defines the &epsilon;-neighborhood of a point
 *   <li>minPoints: the minimum number of density-connected points required to form a cluster
 * </ul>
 * <p>
 * <b>Note:</b> as DBSCAN is not a centroid-based clustering algorithm, the resulting
 * {@link Cluster} objects will have no defined center.
 *
 * @param <T> type of the points to cluster
 * @see <a href="http://en.wikipedia.org/wiki/DBSCAN">DBSCAN (wikipedia)</a>
 * @see <a href="http://www.dbs.ifi.lmu.de/Publikationen/Papers/KDD-96.final.frame.pdf">
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise</a>
 * @since 3.1
 */
public class DBScanClusterer<T extends Clusterable<T>> {

	/** Maximum radius of the neighborhood to be considered. */
	private final double              eps;

	/** Minimum number of points needed for a cluster. If this value is set to zero, then the algorithm will not any
	 * points as noise.
	 */
	private final int                 minPts;
	private ArrayList<Cluster<T>> clusters;

	Map<Clusterable<T>, PointStatus> visited;

	public List<Cluster<T>> getClusters()
	{
		return this.clusters;
	}

	/** Status of a point during the clustering process. */
	public enum PointStatus {
		/** The point has is considered to be noise. */
		NOISE,
		/** The point is already part of a cluster. */
		PART_OF_CLUSTER
	}

	/**
	 * Creates a new instance of a DBSCANClusterer.
	 *
	 * @param eps maximum radius of the neighborhood to be considered
	 * @param minPts minimum number of points needed for a cluster
	 * @throws NotPositiveException if {@code eps < 0.0} or {@code minPts < 0}
	 */
	public DBScanClusterer(final double eps, final int minPts)
			throws NotPositiveException {
		if (eps < 0.0d) {
			throw new NotPositiveException(eps);
		}
		if (minPts < 0) {
			throw new NotPositiveException(minPts);
		}
		this.eps = eps;
		this.minPts = minPts;
	}

	/**
	 * Returns the maximum radius of the neighborhood to be considered.
	 *
	 * @return maximum radius of the neighborhood
	 */
	public double getEps() {
		return eps;
	}

	/**
	 * Returns the minimum number of points needed for a cluster.
	 *
	 * @return minimum number of points needed for a cluster
	 */
	public int getMinPts() {
		return minPts;
	}

	/**
	 * Performs DBSCAN cluster analysis.
	 * <p>
	 * <b>Note:</b> as DBSCAN is not a centroid-based clustering algorithm, the resulting
	 * {@link Cluster} objects will have no defined center.
	 *
	 * @param points the points to cluster
	 * @return the list of clusters
	 * @throws NullArgumentException if the data points are null
	 */
	public List<Cluster<T>> cluster(final Collection<T> points) throws NullArgumentException {

		// sanity checks
		MathUtils.checkNotNull(points);

		clusters = new ArrayList<Cluster<T>>();
		visited = new HashMap<>();

		for (final T point : points) {
			if (visited.get(point) != null) {
				continue;
			}
			final List<T> neighbors = getNeighbors(point, points);
			if (neighbors.size() >= minPts) {
				// DBSCAN does not care about center points
				final Cluster<T> cluster = new Cluster<T>();
				clusters.add(expandCluster(cluster, point, neighbors, points, visited));
			} else {
				visited.put(point, PointStatus.NOISE);
			}
		}

		return clusters;
	}

	public ArrayList<T> getNoises()
	{
		ArrayList<T> retval = new ArrayList<>();
		visited.forEach((tClusterable, pointStatus) ->
		{
			if(pointStatus == PointStatus.NOISE)
				retval.add((T) tClusterable);
		}
		);
		return retval;
	}

	/**
	 * Removes a pointer from the set of clusters. This method assumes that the point already belongs to a cluster and
	 * throws a runtime exception otherwise. The method will check the cluster after removing the point and if its size
	 * is less that the {@code minPt} threshold, it will dissolve the cluster and mark all its points as noise.
	 * @param point The point to be removed.
	 * @return
	 */
	public Cluster<T> removePoint(T point)
	{
		Cluster<T> retval = null;
		PointStatus status = visited.remove(point);
		if (status == null)
			throw new RuntimeException("The given point is not seen before.");

		if(status == PointStatus.NOISE)
			return null;

		for (Cluster<T> cluster : clusters)
		{
			List<T> points = cluster.getPoints();
			if(!points.remove(point))
				continue;
			else
				retval = cluster;
			if(points.isEmpty() || points.size() < minPts)
			{
				points.forEach(p -> visited.compute(p, (tClusterable, pointStatus) -> PointStatus.NOISE));
				this.clusters.remove(cluster);
				retval = null;
				break;
			}
		}

		return retval;
	}

	/**
	 * Finds the cluster to which a given {@code point} belongs based on the set of neighboring points {@code neighbor}
	 * whose cluster identity is already identified. This method should only be used when the clusters of neighbor
	 * points is known.
	 * @param point a new point to be assigned to a cluster.
	 * @param neighbors the set of all neighbors of {@code point}. Cluster identity of these points must already be
	 * @return the cluster to which {@code point} belongs or {@code null} if the point is a noise.
	 */
	private Cluster<T> locateCluster(T point, List<T> neighbors)
	{
		HashMap<Cluster<T>, Integer> clusterCounter = new HashMap<>();
		for (T neighbor : neighbors)
		{
			boolean located = false;
			for (Cluster<T> cluster : clusters) {
				if (cluster.getPoints().contains(neighbor)) {
					clusterCounter.compute(cluster, (tCluster, integer) -> integer == null ? 1 : integer + 1);
					located = true;
					break;
				}
			}
			if(located)
				continue;
			if(visited.get(neighbor) == PointStatus.NOISE)
				clusterCounter.compute(null, (tCluster, integer) -> integer == null ? 1: integer + 1);
			else
				throw new RuntimeException("The neighbor " + neighbor.toString() + " does not belong to any existing clusters");

		}

		int max = -1;
		Cluster<T> maxCluster = null;
		for (Map.Entry<Cluster<T>, Integer> entry : clusterCounter.entrySet())
		{
			Cluster<T> cluster = entry.getKey();
			Integer count = entry.getValue();
			if (count > max)
			{
				max = count;
				maxCluster = cluster;
			}
		}

		return maxCluster;
	}

	private int countStatus(List<T> points, PointStatus status)
	{
		int count = 0;
		for (T point :
				points) {
			if(visited.get(point) == status)
				count++;
		}

		return count;
	}

	public ArrayList<Cluster<T>> addPoint(Cluster<T> cluster, T point)
	{
		cluster.addPoint(point);
		visited.put(point, PointStatus.PART_OF_CLUSTER);

		return clusters;
	}

	public int numClustered()
	{
		int retval = 0;
		for(Cluster<T> cluster : clusters)
			retval += cluster.getPoints().size();

		return retval;
	}

	public int numNoise()
	{
		int retval = 0;
		for (Map.Entry<Clusterable<T>, PointStatus> entry : visited.entrySet()) {
			PointStatus pointStatus = entry.getValue();
			if (pointStatus.equals(PointStatus.NOISE))
				retval++;
		}

		return retval;
	}

	public ArrayList<Cluster<T>> addPoint(T point) {
		List<T> clusterables = visited.keySet().stream().map(t -> ((T) t)).collect(Collectors.toList());
		final List<T> neighbors = getNeighbors(point, clusterables);
		if (neighbors.size() >= minPts)
		{
			final Cluster<T> cluster = locateCluster(point, neighbors);
			if(cluster != null)
			{
				cluster.addPoint(point);
				visited.put(point, PointStatus.PART_OF_CLUSTER);
			}
			else
			{
				int count = countStatus(neighbors, PointStatus.NOISE);
				if(count >= minPts)
				{
					Cluster<T> newCluster = new Cluster<>();
					newCluster.addPoint(point);
					clusters.add(newCluster);
					visited.put(point, PointStatus.PART_OF_CLUSTER);
					neighbors.forEach(n -> {
						if(visited.get(n) == PointStatus.NOISE)
						{
							newCluster.addPoint(n);
							visited.put(n, PointStatus.PART_OF_CLUSTER);
						}

					});
				}
				else
					visited.put(point, PointStatus.NOISE);
			}
		}
		else
		{
			visited.put(point, PointStatus.NOISE);
		}

		return clusters;
	}

	/**
	 * Expands the cluster to include density-reachable items.
	 *
	 * @param cluster Cluster to expand
	 * @param point Point to add to cluster
	 * @param neighbors List of neighbors
	 * @param points the data set
	 * @param visited the set of already visited points
	 * @return the expanded cluster
	 */
	private Cluster<T> expandCluster(final Cluster<T> cluster,
									 final T point,
									 final List<T> neighbors,
									 final Collection<T> points,
									 final Map<Clusterable<T>, PointStatus> visited) {
		cluster.addPoint(point);
		visited.put(point, PointStatus.PART_OF_CLUSTER);

		List<T> seeds = new ArrayList<T>(neighbors);
		int index = 0;
		while (index < seeds.size()) {
			final T current = seeds.get(index);
			PointStatus pStatus = visited.get(current);
			// only check non-visited points
			if (pStatus == null) {
				final List<T> currentNeighbors = getNeighbors(current, points);
				if (currentNeighbors.size() >= minPts) {
					seeds = merge(seeds, currentNeighbors);
				}
			}

			if (pStatus != PointStatus.PART_OF_CLUSTER) {
				visited.put(current, PointStatus.PART_OF_CLUSTER);
				cluster.addPoint(current);
			}

			index++;
		}
		return cluster;
	}

	/**
	 * Returns a list of density-reachable neighbors of a {@code point}.
	 *
	 * @param point the point to look for
	 * @param points possible neighbors
	 * @return the List of neighbors
	 */
	private List<T> getNeighbors(final T point, final Collection<T> points) {
		final List<T> neighbors = new ArrayList<T>();
		for (final T neighbor : points) {
			if (point != neighbor && neighbor.distanceFrom(point) <= eps) {
				neighbors.add(neighbor);
			}
		}
		return neighbors;
	}

	/**
	 * Merges two lists together.
	 *
	 * @param one first list
	 * @param two second list
	 * @return merged lists
	 */
	private List<T> merge(final List<T> one, final List<T> two) {
		final Set<T> oneSet = new HashSet<T>(one);
		for (T item : two) {
			if (!oneSet.contains(item)) {
				one.add(item);
			}
		}
		return one;
	}
}
