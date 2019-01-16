package tl.gp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleProblemForm;
import ec.util.*;

/**
 * This program is designed to read a population file, evaluate its members on test domain, backup
 * the population file on an archive folder and save the loaded population file evaluated on test
 * domain on the same path it was loaded from.
 *
 * This program uses the following ECJ parameters: <p>
 * - train-pop-folder (optional)
 * @author mazhar
 *
 */
public class EvaluateOnTest
{
	static EvolutionState state = null;

	/**
	 * This parameter does not have any base parameter.
	 */
	public static final String P_TRAIN_POP_FOLDER = "train-pop-folder";

	/**
	 * When a population file is evaluated, it is stored on the same folder it was loaded from
	 * so that other algorithms can load and use it easier. The file that contains the data from
	 * train domain is moved to the folder that is given by this folder.
	 * The value of this field is given with the parameter 'train-pop-folder'. <p>
	 *
	 * This path is always treated as relative and is appended to the directory that contains the
	 * the population file that is evaluated.
	 */
	static String trainPopulationFolder="TrainPopulation";


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

		trainPopulationFolder = state.parameters.getStringWithDefault(
									new Parameter(P_TRAIN_POP_FOLDER), null, trainPopulationFolder);

        p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
        	state.output.fatal("Sample size is too small: " + samples);
        else
        	state.output.warning("Sample size in AnalyzeTerminals: " + samples);
	}

	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		if(args.length < 2 )
		{
			System.err.println("Invalid number of arguments. Usage: EvaluateOnTest "
					+ " <test param file> <input population file> "
					+ " [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		loadECJ(args[0], Arrays.copyOfRange(args, 2, args.length));


		Path path = Paths.get(args[1]);

		Path trainArchive = Paths.get(path.getParent().toString(), trainPopulationFolder);
		if(!Files.exists(trainArchive) || !Files.isDirectory(trainArchive))
			Files.createDirectories(trainArchive);
		trainArchive = Paths.get(trainArchive.toString(), path.getFileName().toString());


		Population pop = PopulationWriter.loadPopulation(path.toFile());
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
				evaluate(state, (GPIndividual)ind);
			}
		}
		Files.move(path, trainArchive, StandardCopyOption.REPLACE_EXISTING);
		PopulationWriter.savePopulation(pop, path.toString());
	}
//		catch (FileNotFoundException e)
//		{
//			System.err.println("\nFile not found: " + e.getMessage());
//		}
//		catch (ClassNotFoundException e)
//		{
//			System.err.println("A class not found exception occurred. Could not find class of the"
//					+ "object in the saved file. Is the file OK?");
//			e.printStackTrace();
//		}
//		catch(InvalidObjectException e)
//		{
//			System.err.println("File contains objects that are not of type GPIndividual.");
//			e.printStackTrace();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		catch (IllegalArgumentException e)
//		{
//			System.err.println(e.getMessage());
//		}
//	}

	static double evaluate(EvolutionState state, GPIndividual gind)
	{
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);

		return gind.fitness.fitness();
	}

}
