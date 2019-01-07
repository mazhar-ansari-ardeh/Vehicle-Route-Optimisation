package tl.gp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

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

	/**
	 * The percent of the individuals in the source domain to consider to analyzing. The value of
	 * this parameter must be in [0, 1].
	 * If this value is less than 1, the individuals in the population will be sorted according to
	 * their fitness and then will be selected.
	 */
	private static double k;

	static final String P_BASE = "analyze-terminals";

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

        Parameter base = new Parameter(P_BASE);
        p = base.push("k");
        k = state.parameters.getDouble(p, null);
        if(k < 0 || k > 1)
        	state.output.fatal("K percent is not a valid value for AnalyzeTerminal: " + k);
        state.output.warning("K percent is: " + k);
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
		ArrayList<Population> popList = new ArrayList<>();
		try
		{
			//	String inputFileNamePath = "/vol/grid-solar/sgeusers/mazhar/gdb2-v7-to8/1/stats/gdb2-v7-writeknow/population.gen.49.bin";
			String inputFileNamePath = args[1];
			File f = new File(inputFileNamePath);
			if(f.isDirectory())
			{
				Path rootPath = Paths.get(inputFileNamePath);
				ArrayList<Path> regularFilePaths = Files.list(rootPath)
						.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(".bin"))
						.collect(Collectors.toCollection(ArrayList::new));

				regularFilePaths.forEach(System.out::println);
				for(Path path: regularFilePaths)
				{
					popList.add(PopulationWriter.loadPopulation(path.toFile()));
				}
			}
			else
				popList.add(PopulationWriter.loadPopulation(inputFileNamePath));


			state.output.warning(k + " percent of each subpopulation is loaded");
			for(Population pop : popList)
			{
				for(Subpopulation sub : pop.subpops)
				{
					for(Individual gind : sub.individuals)
						evaluate(state, (GPIndividual) gind);
				}

				PopulationWriter.sort(pop);
				for(Subpopulation sub : pop.subpops)
				{
					for(int i = 0; i < Math.round(sub.individuals.length * k); i++)
					{
						if(!(sub.individuals[i] instanceof GPIndividual))
						{
							System.err.println("WARNING: Found and object in the saved population file"
									+ " that is not of type GPIndividual:"
									+ sub.individuals[i].getClass() + " The individule is ignored.");
							continue;
						}
						extractAndSave(state, (GPIndividual)sub.individuals[i]);
						state.output.warning("Finished work on individual: " + i);
					}
				}

				File out = new File(outputFileNamePath);
				ObjectOutputStream os = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(out)));
				os.writeObject(keeper.book);
				os.close();
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

		void add(String terminal, GPIndividual ind, ArrayList<String> allIndTerminals)
		{
			if(book.containsKey(terminal))
			{
				HashMap<GPIndividual, GPIndividualFeatureStatistics> h = book.get(terminal);
				if(h.containsKey(ind))
				{
					GPIndividualFeatureStatistics stat = h.get(ind);
					if(!stat.getTerminal().equals(terminal))
						state.output.fatal("Statistics inconsistency: " + terminal + "!="
											+ stat.getTerminal());

					stat.incrementFrequency();
				}
				else
				{
					Pair<Double, Double> contrib = TreeSlicer.getFeatureContribution(state, ind
															 						 , terminal
															 						 , false);
					GPIndividualFeatureStatistics stats
						= new GPIndividualFeatureStatistics(ind, terminal,allIndTerminals, contrib);
					stats.incrementFrequency();

					h.put(ind, stats);
				}
			}
			else
			{
				Pair<Double, Double> contrib = TreeSlicer.getFeatureContribution(state, ind
					, terminal
					, false);
				GPIndividualFeatureStatistics stats
					= new GPIndividualFeatureStatistics(ind, terminal,allIndTerminals, contrib);
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
		// evaluate(state, gind);
		ArrayList<String> terminals = getTerminals(gind.trees[0].child);

		for(int i = 0; i < terminals.size(); i++)
		{
			String terminal = terminals.get(i);
			keeper.add(terminal, gind, terminals);
		}
	}

	static double evaluate(EvolutionState state, GPIndividual gind)
	{
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);

		return gind.fitness.fitness();
	}

}
