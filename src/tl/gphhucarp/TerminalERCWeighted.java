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

	public static final String P_TERMINAL_FILE = "terminal-file";

	int logID;

	@SuppressWarnings("unchecked")
	@Override
    public void setup(final EvolutionState state, final Parameter base)
	{
        super.setup(state, base);
        logID = setupLogger(state, base);

        Parameter p = base.push(P_TERMINAL_FILE);
        String fileName = state.parameters.getString(p, null);

        File in = new File(fileName);
        ObjectInputStream oi;
		try
		{
			oi = new ObjectInputStream(new FileInputStream(in));
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

	private static double[] sourceUseCount = null;

	double[] calculateWeights(List<GPNode> terminals)
	{
//		weights = new double[terminals.size()];
		sourceUseCount = new double[terminals.size()];

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

			sourceUseCount[i] = useCount;
		}
		weights = Arrays.copyOf(sourceUseCount, sourceUseCount.length);
		RandomChoice.organizeDistribution(weights, true);

		return weights;
	}

	// Maps the terminal to its use count
	HashMap<GPNode, Integer> useCount = new HashMap<>();

	@Override
	public void resetNode(EvolutionState state, int thread)
	{
		TerminalERCEvolutionState tstate = ((TerminalERCEvolutionState)state);
		List<GPNode> terminals = tstate.getTerminalSet(subpop).getList();
		if(weights == null)
		{
			weights = calculateWeights(terminals);
			for(GPNode term : terminals)
				useCount.put(term, 0);
		}

		int winner = RandomChoice.pickFromDistribution(weights,state.random[thread].nextDouble());
		terminal = terminals.get(winner);
		useCount.put(terminal, useCount.get(terminal) + 1);
		log(state, terminal, logID, "terminal: " + terminal, "weight: " + weights[winner]
				 , "useCount: " + useCount.get(terminal));

		if (terminal instanceof ERC) {
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
