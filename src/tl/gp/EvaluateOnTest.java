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
import gphhucarp.gp.GPHHEvolutionState;
import tl.ecj.ECJUtils;

/**
 * This program is designed to read a population file, evaluate its members on test domain and save the evaluated population
 * file on a new path given through commandline.
 *
 * This program does not introduce any new following ECJ parameters. <p>
 * @author mazhar
 *
 */
public class EvaluateOnTest
{
	static EvolutionState state = null;

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

        Parameter p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
        	state.output.fatal("Sample size is too small: " + samples);
        else
        	state.output.warning("Sample size in EvaluateOnTest: " + samples);
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		if(args.length < 3 )
		{
			System.err.println("Invalid number of arguments. Usage: EvaluateOnTest "
					+ " <test param file> <input population file> <output folder>"
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

		/* When a population file is evaluated, it is stored on the this folder so that other algorithms can load and use it
		 * easier.
		 * The value of this variable is read from commandline. This path is can be relative or absolute.
		 */
		Path testedPopulationFolder = Paths.get(args[2]);
		if(!Files.exists(testedPopulationFolder) || !Files.isDirectory(testedPopulationFolder))
			Files.createDirectories(testedPopulationFolder);

		Population pop = PopulationUtils.loadPopulation(inputPopFile.toFile());
		for(Subpopulation sub : pop.subpops)
		{
			for(int i = 0; i < sub.individuals.length; i++)
			{
				Individual ind = sub.individuals[i];
				if(!(ind instanceof GPIndividual))
				{
					System.err.println("WARNING: Found and object in the saved population file"
							+ " that is not of type GPIndividual:" + ind.getClass()
							+ " The individual is ignored.");
					continue;
				}
				evaluate(state, (GPIndividual)ind);
				 state.output.message("Finished evaluating the individual " + i);
			}
		}
		Path outputFile = Paths.get(testedPopulationFolder.toString(), inputPopFile.getFileName().toString());
		PopulationUtils.savePopulation(pop, outputFile.toString());
	}

	static void evaluate(EvolutionState state, GPIndividual gind)
	{
		double fitness = gind.fitness.fitness();
		if(gind instanceof TLGPIndividual)
		{
			TLGPIndividual tlg = (TLGPIndividual) gind;
			if(tlg.isTested())
			{
				state.output.warning("Individual is already tested. Ignoring the individual");
				// return tlg.fitness.fitness();
			}
			tlg.setTested(true);
			tlg.setFitnessOnTrain(fitness);
		}
		else
			state.output.warning("GP individual is not of type TLGPIndividual");

		// If the fitness is infinity, then it usually means that the individual has been cleared.
		if(fitness == Double.POSITIVE_INFINITY || fitness == Double.NEGATIVE_INFINITY)
			return;

		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
		((GPHHEvolutionState)state).resetSeenSituations();

//		return gind.fitness.fitness();
	}

}
