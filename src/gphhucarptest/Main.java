package gphhucarptest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;

import gphhucarp.core.Arc;
import gphhucarp.core.Graph;
import gphhucarp.representation.route.NodeSeqRoute;

public class Main
{
	public static void main(String[] args)
	{
		Arc arc = new Arc(0, 1, 5, 6, 4, null, 0.2, 0.3);
		System.out.println("Expected demand: " + arc.getExpectedDemand());
		System.out.println("Expected deadheading cost: " + arc.getExpectedDeadheadingCost());
		System.out.println("Priority:" + arc.getPriority());
		System.out.println("Is task: " + arc.isTask());
		RandomDataGenerator rdg = new RandomDataGenerator();
		System.out.println("Sample demand: " + arc.sampleDemand(rdg));
		System.out.println(arc.toString());

		Arc arc2 = new Arc(1, 2, 6, 7, 5, null, 0.2, 0.3);
		Arc arc3 = new Arc(2, 3, 1, 1, 8, null, 0.2, 0.3);
		// Graph

		ArrayList<Integer> nodes = new ArrayList<>();
		nodes.add(0);
		nodes.add(1);
		nodes.add(2);
		nodes.add(3);

//		Map<Pair<Integer, Integer>, Arc> arcMap = new HashMap<>();
//		arcMap.put(Pair.of(0, 1), arc);
//		arcMap.put(Pair.of(1, 2), arc2);
//		arcMap.put(Pair.of(2, 3), arc3);
//		// Pair<Integer, Integer>
//		Graph graph = new Graph(nodes, arcMap);
//		System.out.println("Estimated cost between 0 and 1:" + graph.getEstCost(0, 1));

		List<Integer> nodeSeq = new ArrayList<>();
		nodeSeq.add(2);
		nodeSeq.add(3);
		nodeSeq.add(5);
		nodeSeq.add(1);
		List<Double> fracSeq = new ArrayList<>();
		fracSeq.add(1d);
		fracSeq.add(0d);
		fracSeq.add(1d);
		fracSeq.add(0d);
		NodeSeqRoute route = new NodeSeqRoute(5/*capacity*/, 5 /*demand*/, 3 /*cost*/, nodeSeq, fracSeq);
		System.out.println(route.toString());
	}
}
