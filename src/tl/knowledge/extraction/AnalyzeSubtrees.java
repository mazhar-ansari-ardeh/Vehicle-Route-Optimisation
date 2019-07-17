package tl.knowledge.extraction;

import ec.*;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import javafx.util.Pair;
import tl.TLLogger;
import tl.gp.PopulationUtils;
import tl.gp.TLGPIndividual;
import tl.gp.TreeSlicer;
import tl.gp.hash.AlgebraicHashCalculator;
import tl.gp.hash.HashCalculator;
import tl.knowledge.KnowledgeExtractionMethod;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * Analyzes subtrees of GP individuals in a population file or all the population files in a folder
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
 * AnalyzeSubtrees <test param file> <input population file/folder> <output file> [<EJC params>...]"
 *
 * ECJ parameters are optional but important parameters, like sample size, can be passed to this
 * program this way. A sample command line running of the program: <p>
 * AnalyzeSubtrees bin/tl/gphhucarp/source.param \
 *   /home/mazhar/MyPhD/SourceCodes/gpucarp/stats/source/population.gen.0.bin \
 *   subtree-book.bk eval.problem.eval-model.instances.0.samples=500 analyze-terminals.percent=0.5
 * @author mazhar
 *
 */
//@SuppressWarnings("Duplicates")
@SuppressWarnings("ALL")
public class AnalyzeSubtrees
{
	static EvolutionState state = null;

	/**
	 * The percent of the individuals in the source domain to consider to analyzing. The value of
	 * this parameter must be in [0, 1].
	 * If this value is less than 1, the individuals in the population will be sorted according to
	 * their fitness and then will be selected.
	 */
	private static double percent;

	public static final String P_BASE = "analyze-subtrees";

	public static final String P_PERCENT = "percent";

	/**
	 * Total number of generations on the source domain. This parameter is counted from 1.
	 */
	public static final String P_NUM_GENERATIONS = "num-generations";
	private static int numGenerations = -1;

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


	/** Extraction method. The acceptable methods are {@code KnowledgeExtractionMethod.AllSubtrees},
	 * and {@code KnowledgeExtractionMethod.RootSubtree}.
	 */
	public static final String P_KNOWLEDGE_EXTRACTION = "knowledge-extraction";
	static KnowledgeExtractionMethod extractionMethod;

	/**
	 * Minimum allowed size of code fragments to use, inclusive.
	 */
	public static final String P_MIN_ST_DEPTH = "min-st-depth";
	private static int minDepth;

	/**
	 * Maximum allowed size of code fragments to use, inclusive.
	 */
	public static final String P_MAX_ST_DEPTH = "max-st-depth";
	private static int maxDepth;

    /**
	 * This program implements a simple niching algorithm and this parameter indicates its radius.
	 */
	public static final String P_FITNESS_NICHE_RADIUS = "fitness-niche-radius";

	private static double fitnessNicheRadius;

    private static int toGeneration = -1;

	/**
	 * Number of individuals that were analyzed.
	 */
	static int numAnalyzed = 0;

    //	public static final String P_SIMPLIFY = "simplify";
    //	 */
    //	 * simplify the GP tree before analyzing features. NOT IMPLEMENTED.
    //	 * If {@code true}, the program will use the algorithm implemented in {@code AlgebraicTreeSimplifier} to
//	/**

    //	public static final String P_USE_EQUIVALENCY_FILTER = "use-equival-filter";
    //	 */
    //	 * {@code AlgebraicTreeSimplifier} to identify similar trees and ignore them. NOT IMPLEMENTED.
    //	 * If {@code true}, the program will use the hashing algorithm implemented in
//	/**


    private static int fromGeneration = -1;

	private static TLLogger<GPNode> logger;

	private static int logID;

	private static HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> book = new HashMap<>();

	private static void loadECJ(String paramFileNamePath, String... ecjParams)
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
        	state.output.fatal("K percent is not a valid value for AnalyzeSubtrees: " + percent);
        state.output.warning("Percent is: " + percent);

        fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
        state.output.warning("From generation: " + fromGeneration);
        toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
        state.output.warning("To generation: " + toGeneration);
        fitnessNicheRadius = state.parameters.getDouble(base.push(P_FITNESS_NICHE_RADIUS), null);
        state.output.warning("Niche radius: " + fitnessNicheRadius);

        p = base.push(P_KNOWLEDGE_EXTRACTION);
		String extraction = state.parameters.getString(p, null);
		if(extraction == null)
			state.output.fatal("Extraction method cannot be null");
		extractionMethod = KnowledgeExtractionMethod.parse(extraction);
		state.output.warning("Extraction method: " + extractionMethod);

		p = base.push(P_MIN_ST_DEPTH);
		minDepth = state.parameters.getInt(p, null);
		state.output.warning("Min depth: " + minDepth);
		p = base.push(P_MAX_ST_DEPTH);
		maxDepth = state.parameters.getInt(p, null);
		state.output.warning("Max depth: " + maxDepth);

        p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
        	state.output.fatal("Sample size is too small: " + samples);
        else
        	state.output.warning("Sample size in AnalyzeSubtrees: " + samples);

        logger = new TLLogger<GPNode>()
		{
		};

		logID = logger.setupLogger(state, base);

		p = base.push(P_NUM_GENERATIONS);
		numGenerations = state.parameters.getInt(p, null);
		state.output.warning("Number of generations on source domain: " + numGenerations);
		state.output.warning("AnalyzeSubtrees loaded.");
	}


	public static void main(String[] args)
	{
		if(args.length < 3 )
		{
			System.err.println("Invalid number of arguments. Usage: AnalyzeSubtrees "
					+ " <test param file> <input population file/folder>"
					+ " <output file> [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		loadECJ(args[0], Arrays.copyOfRange(args, 3, args.length));

		HashCalculator hs = new AlgebraicHashCalculator(state, 0, 100003621);
//		AlgebraicTreeSimplifier sim = new AlgebraicTreeSimplifier(hs);

		String outputFileNamePath = args[2];
		ArrayList<Population> popList = new ArrayList<>();
		try
		{
			//	String inputFileNamePath = "/vol/grid-solar/sgeusers/mazhar/gdb2-v7-to8/1/stats/gdb2-v7-writeknow/population.gen.49.bin";
			String inputFileNamePath = args[1];
			File f = new File(inputFileNamePath);
			double minFit = Double.MAX_VALUE;
			double maxFit = Double.MIN_VALUE;
			if(f.isDirectory())
			{
				if(fromGeneration <= -1 || toGeneration <= -1)
				{
					logger.log(state, logID, "Generation range is invalid: " + fromGeneration
							+ " to " + toGeneration + "\n");
					state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
																		+  toGeneration);
				}
				for(int i = 0; i < numGenerations; i++)
				{
					Population p = PopulationUtils.loadPopulation(
							Paths.get(inputFileNamePath, "population.gen." + i + ".bin").toFile());
					p = PopulationUtils.sort(p);
					double fit = p.subpops[0].individuals[0].fitness.fitness();
					if(fit > maxFit)
						maxFit = fit;
					if(fit < minFit)
						minFit = fit;
					popList.add(p);
				}
			}
			else
			{
				fromGeneration = 0;
				toGeneration = 0;
				popList.add(PopulationUtils.loadPopulation(inputFileNamePath));
			}

            logger.log(state, logID, percent + " percent of each subpopulation is loaded\n");
            for(int gen = fromGeneration; gen <= toGeneration; gen++)
			{
                Population pop = popList.get(gen);
                for(Subpopulation sub : pop.subpops)
				{
                    double fitness = -1; // sub.individuals[0].fitness.fitness();
                    for(int i = 0; i < Math.round(sub.individuals.length * percent); i++)
					{
                        if(!(sub.individuals[i] instanceof GPIndividual))
						{
                            System.err.println("WARNING: Found and object in the saved population "
									+ " file that is not of type GPIndividual:"
									+ sub.individuals[i].getClass()+ " The individual is ignored.");
                            logger.log(state, logID,
									"WARNING: Found and object in the saved population file"
									+ " that is not of type GPIndividual:"
									+ sub.individuals[i].getClass() + " The individual is ignored."
									+ "\n");
                            continue;
                        }
                        GPIndividual ind = (GPIndividual)sub.individuals[i];
                        logger.log(state, logID, "Processing individual " + i + " with hash value: "
								   + hs.hashOfTree(ind.trees[0].child) + " and fitness: "
								   + ind.fitness.fitness() + "\n");

						if(Math.abs(fitness - ind.fitness.fitness()) <= fitnessNicheRadius)
						{
							logger.log(state, logID, "Individual ignored due to falling in niche: "
										+ (ind).trees[0].child.makeCTree(true, true, true)
										+ ", fitness: " + ind.fitness.fitness() + "\n\n");

							continue;
						}
						else // a new niche detected.
							fitness = ind.fitness.fitness();

						extractAndSave(state, ind);
						logger.log(state, logID, "Finished work on individual: " + i + "\n");
					}
				}

			}

			File out = new File(outputFileNamePath);
			ObjectOutputStream os = new ObjectOutputStream(
					new BufferedOutputStream(new FileOutputStream(out)));
			os.writeDouble(minFit);
			os.writeDouble(maxFit);
			os.writeObject(book);
			os.close();

			logger.log(state, logID, "Finished analyzing. Total number of individuals processed: " + numAnalyzed + ".\n");
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

	private static void extractAndSave(EvolutionState state, GPIndividual gind)
	{
		if(gind instanceof TLGPIndividual)
		{
			if(!((TLGPIndividual) gind).isTested())
			{
				logger.log(state, logID, "GPIndividual is not evaluated on test scenario.\n");
				state.output.fatal("GPIndividual is not evaluated on test scenario");
			}
		}
		else
			logger.log(state, logID, "GPIndividual is not of type TLGPIndividual.\n");

		ArrayList<Pair<GPNode, Double>> subtrees;
		switch(extractionMethod)
		{
		case Root:
			state.output.fatal("Root extraction method is not supported.");
		case RootSubtree:
			subtrees = TreeSlicer.sliceRootSubTreesWithContrib(state, gind, minDepth, maxDepth);
			break;
		case AllSubtrees:
			subtrees = TreeSlicer.sliceAllWithContrib(state, gind, false, minDepth, maxDepth);
			break;
		default:
			throw new IllegalArgumentException("Given extraction method is not supported: "
					+ extractionMethod);
		}

		book.put(gind, subtrees);
		numAnalyzed++;
	}
}
