package tl.knowledge.ppt.pipe;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import org.apache.commons.lang3.ArrayUtils;
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
     * The size of the set that is sampled from the population set to learn from for the frequency-based learning
     * method.
     */
    public static final String P_SAMPLE_SIZE = "sample-size";

    /**
     * The tournament size that is used by the frequency-based learning method.
     */
    public final static String P_TOURNAMENT_SIZE = "tournament-size";

    /**
     * The learning rate. The meaning of this parameter may be different for different learning algorithms.
     */
    public static final String P_LEARNING_RATE = "lr";

    public static FrequencyLearner newFrequencyLearner(EvolutionState state, Parameter base, String[] functions, String[] terminals)
    {
        Parameter p = base.push(P_SAMPLE_SIZE);
        int sampleSize = state.parameters.getInt(p, null);
        if(sampleSize <= 0)
            state.output.fatal("Sample size must be a positive value: " + sampleSize);
        else
            state.output.warning("Sample size: " + sampleSize);

        p = base.push(P_TOURNAMENT_SIZE);

        // The tournament size to sample individuals from the population to learn.
        int tournamentSize = state.parameters.getInt(p, null);
        if(tournamentSize > sampleSize)
            state.output.fatal("Tournament size must be positive and smaller than sample size: " + tournamentSize);
        else
            state.output.warning("Tournament size: " + tournamentSize);

        p = base.push(P_LEARNING_RATE);

        // The learning rate for learning.
        double lr = state.parameters.getDouble(p, null);
        if(lr < 0 || lr > 1)
            state.output.fatal("The value of the learning rate is invalid:" + lr);
        else
            state.output.warning("Learning rate: " + lr);

        return new FrequencyLearner(state, 0, functions, terminals, lr, sampleSize, tournamentSize);
    }

    private final static long serialVersionUID = -7886760246174760589L;

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
     * @param tournamentSize the tournament size to use for selecting from the population to learn from. A zero or
     *                       selection of size negative value of this parameter will disable tournament selection and will
     *                       use a ranked selection of size {@code sampleSize}.
     */
    public FrequencyLearner(EvolutionState state, int threadNum, String[] functions, String[] terminals, double learningRate,
                            int sampleSize, int tournamentSize)
    {
        this.useTournament = tournamentSize > 0;
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
            if(ind.trees[treeIndex].child.depth() <= 2 || ind.fitness.fitness() == Double.POSITIVE_INFINITY)
                continue;
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

    boolean useTournament = false;
    @Override
    public void adaptTowards(PPTree tree, GPIndividual[] individual, int treeIndex)
    {
        ArrayList<GPIndividual> Ss; // Sampled individuals. The name is from the paper.

        if(useTournament)
        {
            Ss = new ArrayList<>();
            if (individual.length <= sampleSize)
                Ss.addAll(Arrays.asList(individual));
            else
            {
                for (int i = 0; i < sampleSize; i++)
                {
                    int selected = PopulationUtils.tournamentSelect(individual, state, threadnum, tournamentSize);
//                    while (Ss.contains(individual[selected]))
//                        selected = PopulationUtils.tournamentSelect(individual, state, threadnum, tournamentSize);
                    Ss.add(individual[selected]);
                    individual = ArrayUtils.remove(individual, selected);
                }
            }
        }
        else
        {
            Ss = PopulationUtils.rankSelect(individual, sampleSize);
        }
        HashMap<String, HashMap<String, Double>> stats = calculateDistributionEstimate(Ss, treeIndex);
        for(String address : stats.keySet())
        {
            HashMap<String, Double> nodeStat = stats.get(address);
            for(String nodeName : this.functions)
            {
                Double stat = nodeStat.get(nodeName);
                if(stat == null)
                    stat = 0d;

                double oldProbability = tree.getProbabilityOf(address, nodeName);
                double newProbability = (lr * oldProbability) + ((1 - lr) *  stat);
                tree.setProbabilityOf(address, nodeName, newProbability);
            }

            for(String nodeName : this.terminals)
            {
                Double stat = nodeStat.get(nodeName);
                if(stat == null)
                    stat = 0d;

                double oldProbability = tree.getProbabilityOf(address, nodeName);
                double newProbability = (lr * oldProbability) + ((1 - lr) *  stat);
                tree.setProbabilityOf(address, nodeName, newProbability);
            }
        }
    }

    /**
     * Initializes a given probability vector. This class sets the probability of all items to zero.
     * @param vector the vector to initialize.
     */
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
