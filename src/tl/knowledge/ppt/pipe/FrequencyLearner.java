package tl.knowledge.ppt.pipe;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.gp.GPIndividualUtils;
import tl.gp.PopulationUtils;

import java.util.*;


/**
 * Learns the PPT of a population based on the frequency with which each terminal/function appears in GP individuals. This
 * class implements the learning method used in the paper:
 *
 * <p> "Yanai, Kohsuke, and Hitoshi Iba. "Program evolution by integrating EDP and GP." Genetic and Evolutionary Computation
 * Conference. Springer, Berlin, Heidelberg, 2004."
 */
public class FrequencyLearner implements IPIPELearner<GPIndividual[]>
{

    /**
     * The name of all the GP terminals
     */
    private final String[] terminals;

    /**
     * The name of all the GP functions
     */
    private final String[] functions;

    /**
     * The size of the sample from the population to learn from.
     */
    private final int sampleSize;

    private int tournamentSize;

    /**
     * The evolution state object of ECJ.
     */
    private final transient EvolutionState state;

    /**
     * The ECJ thread that is running this learner.
     */
    private final int threadnum;

    /**
     * The learning rate. It is the parameter 'lr' in the paper.
     */
    private double lr;

    /**
     * Constructs a new instance.
     * @param state the {@code EvolutionState} that is governing this evolutionary process.
     * @param threadNum the index of the thread that is running this evolutionary process.
     * @param functions the name of all the GP functions.
     * @param terminals the name of all the GP terminals
     * @param learningRate the learning rate
     * @param sampleSize the size of the sample from the population to learn from.
     * @param tournamentSize the tournament size to use for selecting from the population to learn from.
     */
    public FrequencyLearner(EvolutionState state, int threadNum, String[] functions, String[] terminals, double learningRate,
                            int sampleSize, int tournamentSize)
    {
        if(tournamentSize <= 0)
            throw new IllegalArgumentException("Tournament size must be a positive number.");
        if(sampleSize <= 0)
            throw new IllegalArgumentException("Sample size must be a positive number.");
        this.state = state;
        this.threadnum = threadNum;
        this.functions = functions;
        this.terminals = terminals;
        this.lr = learningRate;
        this.tournamentSize = tournamentSize;
        this.sampleSize = sampleSize;
    }

    private static boolean isdigit(String str)
    {
        try
        {
            Double.parseDouble(str);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private HashMap<String, HashMap<String, Double>> calculateDistributionEstimate(ArrayList<GPIndividual> sample, int treeIndex)
    {
//        PPTree retval = new PPTree(this, this.functions, this.terminals);

        // (nodeAddress, (nodeName, nodeCount))
        HashMap<String, HashMap<String, Double>> stats = new HashMap<>();

        for(GPIndividual ind : sample)
        {
            Map<String, GPNode> indice = GPIndividualUtils.index(ind.trees[treeIndex].child);
            for(String address : indice.keySet())
            {
//                if(address.equals("-1"))
//                    System.out.println(address + ":" + indice.get(address));
                String nodeName = indice.get(address).toString();
                if(isdigit(nodeName))
                    nodeName = "ERC";
                HashMap<String, Double> stat = stats.get(address);
                if(stat == null)
                {
                    stat = new HashMap<>();
                    stat.put(nodeName, 0d);
                    stats.put(address, stat);
                }
                stat.putIfAbsent(nodeName, 0d);
                stat.put(nodeName, stat.get(nodeName) + 1);
            }
        }

        // (nodeName, probability)
//        HashMap<String, Double> probs = new HashMap<>();
        for(String address : stats.keySet())
        {
            HashMap<String, Double> stat = stats.get(address);
            double sum = stat.values().stream().mapToDouble((x)-> x).sum();
            for(String nodeName : stat.keySet())
            {
                HashMap<String, Double> ns = stats.get(address);
                ns.put(nodeName, ns.get(nodeName)/sum);
            }
        }

        return stats;
    }

    @Override
    public void adaptTowards(PPTree tree, GPIndividual[] individual, int treeIndex)
    {
        ArrayList<GPIndividual> Ss = new ArrayList<>(); // Sampled individuals. The name is from the paper.
        if(individual.length <= sampleSize)
            Ss.addAll(Arrays.asList(individual));
        else
            for(int i = 0; i < sampleSize; i++)
            {
                int selected = PopulationUtils.tournamentSelect(individual, state, threadnum, tournamentSize);
                while(Ss.contains(individual[selected]))
                    selected = PopulationUtils.tournamentSelect(individual, state, threadnum, tournamentSize);

                Ss.add(individual[selected]);
            }

        HashMap<String, HashMap<String, Double>> stats = calculateDistributionEstimate(Ss, treeIndex);
        for(String address : stats.keySet())
        {
            HashMap<String, Double> nodeStat = stats.get(address);
            for(String nodeName : nodeStat.keySet())
            {
                double oldProbability = tree.getProbabilityOf(address, nodeName);
                double newProbability = (lr * oldProbability)
                        + ((1 - lr) * nodeStat.get(nodeName));
                tree.setProbabilityOf(address, nodeName, newProbability);
            }
        }
    }

    @Override
    public void initialize(ProbabilityVector vector)
    {
        if(vector == null)
            throw new NullPointerException("The probability vector cannot be null.");

        if(vector.isInitialized())
            return;

        // TODO: How should 'R' be updated?
//        vector.R = state.random[this.threadnum].nextDouble();

        for(String tname : this.terminals)
            vector.probabilities.put(tname, 0d);

        for(String fname : this.functions)
            vector.probabilities.put(fname, 0d);

        vector.setInitialized();
    }
}
