package tl.gp;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleProblemForm;
import ec.util.*;
import gputils.terminal.TerminalERCUniform;
import javafx.util.Pair;
import tl.gphhucarp.GPIndividualFeatureStatistics;

public class AnalyzeTerminals
{
	static EvolutionState state = null;

	static void loadECJ(String paramFileNamePath, String... ecjParams)
	{
		ArrayList<String> params = new ArrayList<>();
		params.add("-file");
		params.add(paramFileNamePath);
		for(String param : ecjParams)
		{
			params.add("-p");
			params.add(param);
		}
		String[] processedParams = new String[params.size()];
		params.toArray(processedParams);
        ParameterDatabase parameters = Evolve.loadParameterDatabase(processedParams);

        state = Evolve.initialize(parameters, 0);

        Parameter p;

        // setup the evaluator, essentially the test evaluation model
        p = new Parameter(EvolutionState.P_EVALUATOR);
        state.evaluator = (Evaluator)
                (parameters.getInstanceForParameter(p, null, Evaluator.class));
        state.evaluator.setup(state, p);
	}

	public static void main(String[] args)
	{
		if(args.length < 3 )
		{
			System.err.println("Invalid number of arguments. Usage: AnalyzeTerminals "
					+ " <test param file> <input population file>"
					+ " <output file> [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		loadECJ(args[0], Arrays.copyOfRange(args, 3, args.length));

		String outputFileNamePath = args[2];
		try
		{
			String inputFileNamePath = args[1];
			Population pop = PopulationWriter.loadPopulation(inputFileNamePath);
			for(Subpopulation sub : pop.subpops)
			{
				for(Individual ind : sub.individuals)
				{
					if(!(ind instanceof GPIndividual))
					{
						System.err.println("WARNING: Found and object in the saved population file"
								+ " that is not of type GPIndividual:" + ind.getClass()
								+ " The individule is ignored.");
						continue;
					}
					extractAndSave(state, (GPIndividual)ind);

				}
			}

			for(String node : keeper.book.keySet())
			{
				System.out.println(node);
				HashMap<GPIndividual, GPIndividualFeatureStatistics> h = keeper.book.get(node);
				System.out.println("Number of inds containing this: " + h.size());
				int terminalUsage = 0;
				for(GPIndividual gind : h.keySet())
				{
					// System.out.println(gind.fitness.fitness() + ", " + h.get(gind));
					terminalUsage += h.get(gind).getFrequency();
				}
				System.out.println("Total usage: " + terminalUsage);
			}

			File out = new File(outputFileNamePath);
			ObjectOutputStream os = new ObjectOutputStream(
					new BufferedOutputStream(new FileOutputStream(out)));
			os.writeObject(keeper.book);
			os.close();

//			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(out));
//			Object o = oi.readObject();
//			BookKeeper bk = (BookKeeper) oi.readObject();
//			oi.close();
//			System.out.println(bk.book.size());
		}
		catch (FileNotFoundException e)
		{
			System.err.println("\nFile not found: " + e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			System.err.println("A class not found exception occurred. Could not find class of the"
					+ "object in the saved file. Is the file OK?");
			e.printStackTrace();
		}
		catch(InvalidObjectException e)
		{
			System.err.println("File contains objects that are not of type GPIndividual.");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	static ArrayList<String> getTerminals(GPNode root)
	{
		ArrayList<String> retval = new ArrayList<>();
		String nodeName = "";
		if(root.children == null || root.children.length == 0)
		{
			if((((TerminalERCUniform)root).getTerminal() instanceof ERC))
			{
				nodeName = "ERC";
//				retval.add(erc);
//				return retval;
			}
			else
				nodeName = ((TerminalERCUniform)root).getTerminal().name();

			retval.add(nodeName);
			return retval;
		}

		for(GPNode node : root.children)
			retval.addAll(getTerminals(node));

		return retval;
	}

	static class BookKeeper implements Serializable
	{
		private static final long serialVersionUID = 1L;

		HashMap<String, HashMap<GPIndividual, GPIndividualFeatureStatistics>> book = new HashMap<>();

		void add(String terminal, GPIndividual ind)
		{
			if(book.containsKey(terminal))
			{
				HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal);
				if(h.containsKey(ind))
				{
					h.get(ind).incrementFrequency();
//					h.put(ind, h.get(ind)+1);
				}
				else
				{
					Pair<Double, Double> contrib = TreeSlicer.getFeatureContribution(state, ind
															 						 , terminal);
					GPIndividualFeatureStatistics stats
						= new GPIndividualFeatureStatistics(contrib);

					h.put(ind, stats);
				}
			}
			else
			{
				Pair<Double, Double> contrib = TreeSlicer.getFeatureContribution(state, ind
					, terminal);
				GPIndividualFeatureStatistics stats
					= new GPIndividualFeatureStatistics(contrib);
				HashMap<GPIndividual, GPIndividualFeatureStatistics> h = new HashMap<GPIndividual
															, GPIndividualFeatureStatistics>();
				h.put(ind, stats);
				book.put(terminal, h);
			}
		}
	}

	static BookKeeper keeper = new BookKeeper();

	private static void extractAndSave(EvolutionState state, GPIndividual gind)
	{
		ArrayList<String> terminals = getTerminals(gind.trees[0].child);

		for(int i = 0; i < terminals.size(); i++)
		{
			String terminal = terminals.get(i);
			keeper.add(terminal, gind);
		}
	}

	static double evaluate(EvolutionState state, GPIndividual gind)
	{
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);

		return gind.fitness.fitness();
	}

}
