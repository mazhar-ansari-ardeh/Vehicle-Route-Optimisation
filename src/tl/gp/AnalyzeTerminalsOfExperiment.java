package tl.gp;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import ec.*;
import ec.gp.*;
import ec.util.*;
import gputils.terminal.TerminalERCUniform;
import javafx.util.Pair;
import tl.TLLogger;
import tl.gp.hash.AlgebraicHashCalculator;
import tl.gp.hash.HashCalculator;
import tl.gphhucarp.GPIndividualFeatureStatistics;


/**
 * Analyzes terminals of GP individuals in a population file or all the population files in a folder
 * and saves the result as a dictionary in a file that is specified as a command line argument. <p>
 * <b>Note</b>: This program assumes that a GP individuals that this program loads are already
 * evaluated on test domain and therefore, it does not evaluate them when it starts its work. On the
 * other hand, if the program needs to evaluate an individual, it does the evaluation, assuming that
 * it has given parameters for test environments.<p>
 *
 * This program introduces the parameter base 'analyze-terminals' and requires the a parameter
 * 'percent' which is the percent of each population to consider for analysis.
 *
 * The program receives its requirements from command line. Usage:
 * 		AnalyzeTerminals <test param file> <input population file/folder> <output file> [<EJC params>...]"
 *
 * ECJ parameters are optional but important parameters, like sample size, can be passed to this
 * program this way. A sample command line running of the program: <p>
 * AnalyzeTerminals bin/tl/gphhucarp/source.param /home/mazhar/MyPhD/SourceCodes/gpucarp/stats/source/population.gen.0.bin book.bk eval.problem.eval-model.instances.0.samples=500 analyze-terminals.percent=0.5
 * @author mazhar
 *
 */
public class AnalyzeTerminalsOfExperiment
{
	static EvolutionState state = null;

	/**
	 * The percent of the individuals in the source domain to consider to analyzing. The value of
	 * this parameter must be in [0, 1].
	 * If this value is less than 1, the individuals in the population will be sorted according to
	 * their fitness and then will be selected.
	 */
	private static double percent;

	public static final String P_BASE = "analyze-terminals";

	public static final String P_PERCENT = "percent";

	/**
	 * When the knowledge source is a directory, this parameter specifies from which generation on
	 * the populations should be read. This parameter is inclusive.
	 */
	public static final String P_GENERATION_FROM = "from-generation";

	/**
	 * When the knowledge source is a directory, this parameter specifies the generation until which
	 * the populations should be read. This parameter is inclusive.
	 */
	public static final String P_GENERATION_TO = "to-generation";

	/**
	 * This program implements a simple niching algorithm and this parameter indicates its radius.
	 */
	public static final String P_FITNESS_NICHE_RADIUS = "fitness-niche-radius";

	/**
	 * If {@code true}, the program will use the algorithm implemented in {@code AlgebraicTreeSimplifier} to
	 * simplify the GP tree before analyzing features. NOT IMPLEMENTED.
	 */
	public static final String P_SIMPLIFY = "simplify";

	/**
	 * If {@code true}, the program will use the hashing algorithm implemented in
	 * {@code AlgebraicTreeSimplifier} to identify similar trees and ignore them. NOT IMPLEMENTED.
	 */
	public static final String P_USE_EQUIVALENCY_FILTER = "use-equival-filter";


	static int fromGeneration = -1;

	static int toGeneration = -1;

	private static double fitnessNicheRadius;

	static TLLogger<GPNode> logger;

	private static int logID;

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
        p = base.push(P_PERCENT);
        percent = state.parameters.getDouble(p, null);
        if(percent < 0 || percent > 1)
        	state.output.fatal("K percent is not a valid value for AnalyzeTerminal: " + percent);
        state.output.warning("Percent is: " + percent);

        fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
        state.output.warning("From generation: " + fromGeneration);
        toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
        state.output.warning("To generation: " + toGeneration);
        fitnessNicheRadius = state.parameters.getDouble(base.push(P_FITNESS_NICHE_RADIUS), null);
        state.output.warning("Niche radius: " + fitnessNicheRadius);

        p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
        	state.output.fatal("Sample size is too small: " + samples);
        else
        	state.output.warning("Sample size in AnalyzeTerminals: " + samples);
        state.output.warning("AnalyzeTerminals loaded.");

        logger = new TLLogger<GPNode>()
		{
		};

		logID = logger.setupLogger(state, base);
	}

	public static void main(String[] args)
	{
		if(args.length < 3 )
		{
			System.err.println("Invalid number of arguments. Usage: AnalyzeTerminals "
					+ " <test param file> <input population file/folder>"
					+ " <output file> [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		loadECJ(args[0], Arrays.copyOfRange(args, 4, args.length));

		HashCalculator hs = new AlgebraicHashCalculator(state, 0, 100003621);
//		AlgebraicTreeSimplifier sim = new AlgebraicTreeSimplifier(state, 0);

		String outputFileNamePath = args[3];
		ArrayList<Population> popList = new ArrayList<>();
		try
		{
			//	String inputFileNamePath = "/vol/grid-solar/sgeusers/mazhar/gdb2-v7-to8/1/stats/gdb2-v7-writeknow/population.gen.49.bin";
			String inputFileNamePath = args[1];
			String knowledgeFolder = args[2];
			File f = new File(inputFileNamePath);
			if(f.isDirectory())
			{
				for(int i = 1; i <= 30; i++)
					popList.add(PopulationUtils.loadPopulation(
							Paths.get(inputFileNamePath, "" + i, "stats", knowledgeFolder
													   , "population.gen.49.bin").toFile()));

//				Path rootPath = Paths.get(inputFileNamePath);
//				ArrayList<Path> regularFilePaths = Files.list(rootPath)
//						.filter(Files::isRegularFile).filter(file -> file.getFileName()
//																		 .toString()
//																		 .endsWith(".bin"))
//						.collect(Collectors.toCollection(ArrayList::new));
//
//				regularFilePaths.forEach(System.out::println);
//				for(Path path: regularFilePaths)
//				{
//					popList.add(PopulationWriter.loadPopulation(path.toFile()));
//				}
			}
			else
				popList.add(PopulationUtils.loadPopulation(inputFileNamePath));


			logger.log(state, logID, percent + " percent of each subpopulation is loaded\n");
			for(Population pop : popList)
			{
				PopulationUtils.sort(pop);
				for(Subpopulation sub : pop.subpops)
				{
					double fitness = -1; // sub.individuals[0].fitness.fitness();
					for(int i = 0; i < Math.round(sub.individuals.length * percent); i++)
					{
						if(!(sub.individuals[i] instanceof GPIndividual))
						{
							System.err.println("WARNING: Found and object in the saved population file"
									+ " that is not of type GPIndividual:"
									+ sub.individuals[i].getClass() + " The individule is ignored.");
							logger.log(state, logID, "WARNING: Found and object in the saved population file"
									+ " that is not of type GPIndividual:"
									+ sub.individuals[i].getClass() + " The individule is ignored." + "\n");
							continue;
						}
						GPIndividual ind = (GPIndividual)sub.individuals[i];
						logger.log(state, logID, "Processing individual " + i + " with hash value: "
								   + hs.hashOfTree(ind.trees[0].child) + " and fitness: "
								   + ind.fitness.fitness() + "\n");

						if(Math.abs(fitness - ind.fitness.fitness()) <= fitnessNicheRadius)
						{
							logger.log(state, logID, "Individual ignored due to falling in niche: " +
									(ind).trees[0].child.makeCTree(true, true, true) + ", fitness: " + ind.fitness.fitness() + "\n\n");
							continue;
						}
						else // a new niche detected.
							fitness = ind.fitness.fitness();

						extractAndSave(state, ind);
						System.out.println("Finished individual " + i);
						logger.log(state, logID, "Finished work on individual: " + i + "\n");
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
				logger.log(state, logID, node + "\n");
				HashMap<GPIndividual, GPIndividualFeatureStatistics> h = keeper.book.get(node);
				System.out.println("Number of inds containing this: " + h.size());
				logger.log(state, logID, "Number of inds containing this: " + h.size() + "\n");
				int terminalUsage = 0;
				for(GPIndividual gind : h.keySet())
				{
					// System.out.println(gind.fitness.fitness() + ", " + h.get(gind));
					terminalUsage += h.get(gind).getFrequency();
				}
				logger.log(state, logID, "Total usage: " + terminalUsage + "\n");
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
					{
						logger.log(state, logID, "Statistics inconsistency: " + terminal + "!="
								+ stat.getTerminal());
						state.output.fatal("Statistics inconsistency: " + terminal + "!="
											+ stat.getTerminal());
					}
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

	// This method is not used any more. Use the program EvaluateOnTest to evaluate a population
	// file before passing it up to this program.
//	static double evaluate(EvolutionState state, GPIndividual gind)
//	{
//		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
//
//		return gind.fitness.fitness();
//	}

}
