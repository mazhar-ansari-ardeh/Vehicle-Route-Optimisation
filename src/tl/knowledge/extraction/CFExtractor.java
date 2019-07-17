package tl.knowledge.extraction;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleProblemForm;
import ec.util.*;
import tl.gp.PopulationUtils;
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.codefragment.fitted.DoubleFittedCodeFragment;

public class CFExtractor
{
	static EvolutionState state = null;

	/**
	 * Maximum size of a code fragment that is allowed to be extracted.
	 * The value of this parameter is read from param file. This parameter is allowed to have a
	 * negative value which means that this parameter should be ignored and code fragments with any
	 * size are allowed to be extracted. <p><p>
	 * If this parameter is zero, no code fragment will be extracted at all.
	 */
	static int maxNodeSize;


	/**
	 * Initialize ECJ. For this purpose, the function loads ECJ with a param file and any additional
	 * features that are passed to the method. Because this class evaluates the (test) fitness of
	 * individuals, the param file should be a relevant to this goal.
	 *
	 * @param paramFileNamePath the path to a param file including the name of the file.
	 * @param ecjParams additional parameters for ECJ.
	 */
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

        p = new Parameter("cfextract.max-size");
        maxNodeSize = parameters.getInt(p, null);
	}

	public static void main(String[] args)
	{
		if(args.length < 4 )
		{
			System.err.println("Invalid number of arguments. Usage: CFExtractor "
					+ " <test param file> <input population file> <extraction method>"
					+ " <output file> [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		loadECJ(args[0], Arrays.copyOfRange(args, 4, args.length));


		String outputFileNamePath = args[3];
		try(ObjectOutputStream oos = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(outputFileNamePath))))
		{
			File inputFileNamePath = new File(args[1]);
			KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(args[2]);

			Population pop = PopulationUtils.loadPopulation(inputFileNamePath);

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
					extractAndSave(state, oos, (GPIndividual)ind, extractionMethod);
				}
			}

			PopulationUtils.savePopulation(pop, outputFileNamePath + ".pop.bin");

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
			System.err.println(e.getMessage());
		}
	}

	private static void extractAndSave(EvolutionState state, ObjectOutputStream oos,
			GPIndividual gind, KnowledgeExtractionMethod extractionMethod) throws IOException
	{
		ArrayList<GPNode> list = null;
		switch(extractionMethod)
		{
		case AllSubtrees:
			list = TreeSlicer.sliceAllToNodes(gind, false);
			break;
		case RootSubtree:
			list = TreeSlicer.sliceRootChildrenToNodes(gind, false);
			break;
		default:
			state.output.fatal("Received an unknown extraction method: " + extractionMethod);
		}

		for(GPNode node : list)
		{
			if(maxNodeSize >= 0 && node.depth() > maxNodeSize)
				continue;

			// Convert node to individual
			GPIndividual newind = gind.lightClone();
			newind.evaluated = false;
			newind.trees[0].child = node;
			node.parent = newind.trees[0].child;

			// Get its fitness
			evaluate(state, newind);
			node.parent = null;

			// Save it to file
			DoubleFittedCodeFragment cf = new DoubleFittedCodeFragment(node,
					null, newind.fitness.fitness());
			// System.out.println("Wrote: " + cf.toString());
			oos.writeObject(cf);
			oos.flush();
		}
	}

	static double evaluate(EvolutionState state, GPIndividual gind)
	{
//		RoutingPolicy routingPolicy =
//                new GPRoutingPolicy(((ReactiveGPHHProblem)state.evaluator.p_problem).getPoolFilter()
//                		, gind.trees[0]);

		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);

//		MultiObjectiveFitness f = new MultiObjectiveFitness();
//        f.objectives = new double[1];
//        f.objectives[0] = -1;

		return gind.fitness.fitness();
	}

}
