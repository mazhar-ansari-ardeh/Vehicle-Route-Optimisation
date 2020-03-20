package tl.knowledge.surrogate.knn.kmeans;

import java.util.*;

import static java.util.stream.Collectors.toSet;


public class Main
{
    private static Random rand = new Random(100);
    private static int min = -100;
    private static int max =  100;

    private static Record random()
    {
//        HashMap<String, Double> features = new HashMap<>();
//        double x = min + rand.nextDouble()*(max - min);
//        double y = min + rand.nextDouble()*(max - min);
//        features.put("x", x);
//        features.put("y", y);
//
//        return new Record(x + ", " + y + "\n", features);

        return null;
    }

    private static List<Record> randomRecords(int count)
    {
        ArrayList<Record> retval = new ArrayList<>(count);

        for( int i = 0; i< count; i++)
        {
            retval.add(random());
        }

        return retval;
    }

    public static void main(String[] args)
    {
//        List<Record> records = randomRecords(1000);
//        long t = System.currentTimeMillis();
//        Map<Centroid, List<Record>> clusters = KMeans.fit(records, 3, new EuclideanDistance(), 1000);
//        System.out.println(System.currentTimeMillis() - t);
//
//        clusters.forEach((key, value) -> {
//            System.out.println("-------------------------- CLUSTER ----------------------------");
//
//            // Sorting the coordinates to see the most significant tags first.
////            System.out.println(sortedCentroid(key));
//            String members = String.join("", value.stream().map(Record::getDescription).collect(toSet()));
//            System.out.print(members);
//
//            System.out.println();
//            System.out.println();
//        });
    }
}