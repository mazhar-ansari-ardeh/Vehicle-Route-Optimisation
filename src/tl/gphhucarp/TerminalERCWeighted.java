package tl.gphhucarp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ec.EvolutionState;
import ec.gp.ERC;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import ec.util.RandomChoice;
import gputils.TerminalERCEvolutionState;
import gputils.terminal.TerminalERC;
import tl.TLLogger;

public class TerminalERCWeighted extends TerminalERC implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;
	protected HashMap<String, HashMap<GPIndividual, GPIndividualFeatureStatistics>> book = null;

	/**
	 * The parameter for the file that contains terminal data from source domain.
	 */
	public static final String P_TERMINAL_FILE = "terminal-file";

	public static final String P_WEIGHT_USE_DEGENERATION_RATE = "weight-use-degeneration-rate";

	public static final String P_WEIGHT_USE_PROBABILITY = "weight-use-probability";

	/**
	 * Acceptable values are all, none and first.
	 */
	public static final String P_WEIGHT_USAGE_POLICY = "weight-use-policy";

	/**
	 * The rate by which probability of using initial value of feature weights, loaded from source
	 * domain, will be decreased. This value is loaded with the
	 * {@code P_INIT_WEIGHT_USE_DEGENERATION_RATE} parameter. If value of this parameter is greater
	 * than 1, it will be interpreted as 1 (which means that the usage probability will not
	 * degrade). If the value of this parameter is less than zero, zero will be considered.
	 * The rate is applied at the end of each generation. As a result, the value of this parameter
	 * is not used in the first generation.
	 */
	double weightUseDegenerationRate;



	protected int logID;

	/**
	 * The probability of using feature weights. A value of 1 will mean that feature weights will
	 * always be used. A value of zero means that feature weights will never be used (even in the
	 * first generation).
	 */
	static double prob = 1;


	/**
	 * Acceptable values are all, none and first.
	 */
	private String policy;

	/**
	 * Maximum value of individual fitness in the source domain. This fitness is assumed to be the
	 * max over all generations.
	 */
	protected double maxFit;

	/**
	 * Maximum value of individual fitness in the source domain. This fitness is assumed to be the
	 * max over all generations.
	 */
	protected double minFit;


	@SuppressWarnings("unchecked")
	@Override
    public void setup(final EvolutionState state, final Parameter base)
	{
        super.setup(state, base);
        logID = setupLogger(state, base);

        Parameter p = base.push(P_WEIGHT_USE_DEGENERATION_RATE);
        weightUseDegenerationRate = state.parameters.getDouble(p, null);
        state.output.warning("init-weight-use-degeneration-rate: " + weightUseDegenerationRate);
        if(weightUseDegenerationRate > 1)
        	weightUseDegenerationRate = 1;
        else if (weightUseDegenerationRate < 0)
        	weightUseDegenerationRate = 0;

        p = base.push(P_WEIGHT_USE_PROBABILITY);
        prob = state.parameters.getDouble(p, null);
        if(prob > 1)
        	prob = 1;
        if(prob < 0)
        	state.output.fatal("Probability value cannot be negative in TerminalERCWeighted");
        state.output.warning("Probability value: " + prob);

        p = base.push(P_WEIGHT_USAGE_POLICY);
        policy = state.parameters.getString(p, null);
        if(policy == null)
        	state.output.fatal("Weight usage policy is not specified or cannot be null");
        policy = policy.toLowerCase().trim();
        if(!policy.equals("all") && !policy.equals("none") && !policy.equals("first"))
        	state.output.fatal("Given policy is invalid:" + policy);
        else
        	state.output.warning("Ploicy loaded: " + policy);

        p = base.push(P_TERMINAL_FILE);
        String fileName = state.parameters.getString(p, null);

        File in = new File(fileName);
        ObjectInputStream oi;
		try
		{
			oi = new ObjectInputStream(new FileInputStream(in));
			minFit = oi.readDouble();
			maxFit = oi.readDouble();
			this.book = (HashMap<String, HashMap<GPIndividual, GPIndividualFeatureStatistics>>)oi.readObject();

			terminal = null;
		} catch (IOException e)
		{
			e.printStackTrace();
			state.output.fatal(e.toString());
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal(e.toString());
		}
		state.output.warning("Loaded terminal file: " + fileName);
		state.output.warning("TerminalERCWeighted loaded");

        terminal = null;
    }

	static double[] weights = null;

	static double[] weightUnnormalized = null;

	double[] calculateWeights(EvolutionState state, List<GPNode> terminals)
	{
//		weights = new double[terminals.size()];
		weightUnnormalized = new double[terminals.size()];

		for (int i = 0; i < terminals.size(); i++)
		{
			//	System.out.println(terminals.get(i) + ", " + terminals.get(i).hashCode());
			//	book.keySet().forEach(node -> System.out.println(node + ", " + node.hashCode()));
			GPNode terminal = terminals.get(i);
			HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal.name());
			if(h == null)
			{
				System.err.println("Terminal " + terminal.name() + " not found in the book shelf.");
				continue;
			}
			int useCount = 0;
			for(GPIndividualFeatureStatistics value : h.values())
				useCount += value.getFrequency();

			weightUnnormalized[i] = useCount;
		}
		weights = Arrays.copyOf(weightUnnormalized, weightUnnormalized.length);
		RandomChoice.organizeDistribution(weights, true);

		return weights;
	}

	// Maps the terminal to its use count
	HashMap<GPNode, Integer> useCount = new HashMap<>();

	private GPNode getTerminal(EvolutionState state, int thread, List<GPNode> terminals)
	{
		switch(policy)
		{
		case "all": // Use feature weights in all generations.
			if(state.random[thread].nextDouble() < prob)
			{
				int winner = RandomChoice.pickFromDistribution(weights,state.random[thread].nextDouble());
				terminal = terminals.get(winner);
				useCount.put(terminal, useCount.get(terminal) + 1);
				log(state, terminal, logID, "terminal: " + terminal, "weight: " + weights[winner]
						 , "useCount: " + useCount.get(terminal));
			}
			else
				terminal = ((TerminalERCEvolutionState)state).pickTerminalUniform(subpop);
			break;
		case "none": // Do not use feature weights.
			terminal = ((TerminalERCEvolutionState)state).pickTerminalUniform(subpop);
			break;
		case "first": // Use feature weights only in the first generation.
			if(state.generation == 0 && state.random[thread].nextDouble() < prob)
			{
				int winner = RandomChoice.pickFromDistribution(weights,state.random[thread].nextDouble());
				terminal = terminals.get(winner);
				useCount.put(terminal, useCount.get(terminal) + 1);
				log(state, terminal, logID, "terminal: " + terminal, "weight: " + weights[winner]
						 , "useCount: " + useCount.get(terminal));
			}
			else
				terminal = ((TerminalERCEvolutionState)state).pickTerminalUniform(subpop);
			break;
		default:
			state.output.fatal("An invalid value for policy found: " + policy);
		}

		return terminal;
	}

	private static int currentGen = 0;

	@Override
	public void resetNode(EvolutionState state, int thread)
	{
		// The condition is true when GP has just advanced into its next generation and therefore,
		// we need to update the weights, ...
		if(currentGen != state.generation)
		{
			if(weightUseDegenerationRate < 0)
				weightUseDegenerationRate = 0;

			prob = prob * weightUseDegenerationRate;

			currentGen = state.generation;
		}

		TerminalERCEvolutionState tstate = ((TerminalERCEvolutionState)state);
		List<GPNode> terminals = tstate.getTerminalSet(subpop).getList();
		if(weights == null)
		{
			weights = calculateWeights(state, terminals);
			for(GPNode term : terminals)
				useCount.put(term, 0);
		}

		terminal = getTerminal(state, thread, terminals);
		if (terminal instanceof ERC)
		{
			terminal = terminal.lightClone();
			terminal.resetNode(state, thread);
		}

	}

	@Override
    public void mutateERC(EvolutionState state, int thread)
	{
        resetNode(state, thread);
    }
}
