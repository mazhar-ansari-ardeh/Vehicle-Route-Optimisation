package tl.knowledge.surrogate.knn;

import ec.gp.GPIndividual;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.gp.similarity.TreeSimilarityMetric;

class KNNPoolItem
{
    GPRoutingPolicy policy;

    public void setDist(TreeSimilarityMetric dist)
    {
        this.dist = dist;
    }

    private TreeSimilarityMetric dist;

    /**
     * Where did this item come from?
     */
    String source;

    /**
     * The fitness of this item when it was first selected to be added to a KNN pool.
     */
    private double originalFitness;

    double fitness;

    /**
     * Number of duplicates of this item. This is the number of duplicates except this object.
     */
    int nDuplicates = 0;

    KNNPoolItem(GPIndividual ind, PoolFilter filter, String source)
    {
        assert ind != null;
        this.source = source;
        policy = new GPRoutingPolicy(filter, ind.trees[0]);
        this.fitness = originalFitness = ind.fitness.fitness();
    }

    /**
     * Returns a string representation of this object in which the fields are separated with a comma. The order of the fields
     * is: source,originalFitness,fitness,nDuplicates,lispTreeofPolicy\n;
     * @return a comma-separated string representation of the fields of this object.
     */
    String toCSVString()
    {
        return source + "," + originalFitness + "," + fitness + "," + nDuplicates + ","
                + policy.getGPTree().child.makeLispTree() + "\n";
    }

    static String CSVHeader()
    {
        return "Source,OriginalFitness,Fitness,nDuplicates,Policy\n";
    }

    public String toString()
    {
        return Double.toString(fitness);
    }


    double getOriginalFitness()
    {
        return originalFitness;
    }

//    @Override
//    public double[] getFeatures()
//    {
//        int[] characterise = dist.characterise(this.policy);
//        double[] retval = new double[characterise.length];
//        for (int i = 0; i < characterise.length; i++)
//        {
//            retval[i] = (double) characterise[i];
//        }
//
//        return retval;
//    }
}
