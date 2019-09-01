package tl.gp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleProblemForm;
import ec.util.*;
import tl.ecj.ECJUtils;

/**
 * This program is designed to read a population file, evaluate its members on test domain, backup
 * the population file on an archive folder and save the loaded population file, evaluated on test
 * domain, on the same path it was loaded from.
 *
 * This program uses the following ECJ parameters: <p>
 * - train-pop-folder (optional)
 * @author mazhar
 *
 */
public class EvaluateOnTest
{
	static EvolutionState state = null;

	// TODO: 1/09/19 Remove this and read it from the command line.
	/**
	 * This parameter does not have any base parameter.
	 */
	public static final String P_TEST_POP_FOLDER = "test-pop-folder";

	/**
	 * When a population file is evaluated, it is stored on the same folder it was loaded from
	 * so that other algorithms can load and use it easier. The file that contains the data from
	 * train domain is moved to the folder that is given by this folder.
	 * The value of this field is given with the parameter 'train-pop-folder'. <p>
	 *
	 * This path is always treated as relative and is appended to the directory that contains the
	 * the population file that is evaluated.
	 */
	private static String testedPopulationFolder ="TestPopulation";


	/**
	 * Initialize ECJ. For this purpose, the function loads ECJ with a param file and any additional
	 * features that are passed to the method. Because this class evaluates the (test) fitness of
	 * individuals, the param file should be a relevant to this goal.
	 *
	 * @param paramFileNamePath the path to a param file including the name of the file.
	 * @param ecjParams additional parameters for ECJ.
	 */
	private static void setup(String paramFileNamePath, String... ecjParams)
	{
		state = ECJUtils.loadECJ(paramFileNamePath, ecjParams);

		testedPopulationFolder = state.parameters.getStringWithDefault(
									new Parameter(P_TEST_POP_FOLDER), null, testedPopulationFolder);

        Parameter p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
        	state.output.fatal("Sample size is too small: " + samples);
        else
        	state.output.warning("Sample size in EvaluateOnTest: " + samples);
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException
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

		setup(args[0], Arrays.copyOfRange(args, 2, args.length));


		Path inputPopFile = Paths.get(args[1]);

		Path testArchive = Paths.get(testedPopulationFolder);
		if(!Files.exists(testArchive) || !Files.isDirectory(testArchive))
			Files.createDirectories(testArchive);
		// testArchive = Paths.get(testArchive.toString(), inputPopFile.getFileName().toString());

		Population pop = PopulationUtils.loadPopulation(inputPopFile.toFile());
		for(Subpopulation sub : pop.subpops)
		{
			for(Individual ind : sub.individuals)
			{
				if(!(ind instanceof GPIndividual))
				{
					System.err.println("WARNING: Found and object in the saved population file"
							+ " that is not of type GPIndividual:" + ind.getClass()
							+ " The individual is ignored.");
					continue;
				}
				evaluate(state, (GPIndividual)ind);
			}
		}
		// Files.move(inputPopFile, testArchive, StandardCopyOption.REPLACE_EXISTING);
		Path outputFile = Paths.get(testArchive.toString(), inputPopFile.getFileName().toString());
		PopulationUtils.savePopulation(pop, outputFile.toString());
	}

	static double evaluate(EvolutionState state, GPIndividual gind)
	{
		if(gind instanceof TLGPIndividual)
		{
			TLGPIndividual tlg = (TLGPIndividual) gind;
			if(tlg.isTested())
			{
				state.output.warning("Individual is already tested. Ignoring the individual");
				return tlg.fitness.fitness();
			}
			tlg.setTested(true);
			tlg.setFitnessOnTrain(((TLGPIndividual) gind).fitness.fitness());
		}
		else
			state.output.warning("GP individual is not of type TLGPIndividual");

		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);

		return gind.fitness.fitness();
	}

}
