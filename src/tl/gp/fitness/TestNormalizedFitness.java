package tl.gp.fitness;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.ReactiveGPHHProblem;
import tl.ecj.ECJUtils;
import tl.gp.FitnessUtils;
import tl.gp.PopulationUtils;
import tl.gp.TLGPIndividual;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This program is designed to read population files that are tested on a test domain, load their individuals and
 * evaluate their members on train domain with the normalised fitness class and then, compare the performance against
 * the test performance.
 *
 * This program does not introduce any new ECJ parameters. <p>
 * @author mazhar
 *
 */
public class TestNormalizedFitness
{
	static EvolutionState state = null;

	/**
	 * Initialize ECJ. For this purpose, the function loads ECJ with a param file and any additional
	 * features that are passed to the method. Because this class evaluates the (test) fitness of
	 * individuals, the param file should be a relevant to this goal.
	 *
	 * The program requires the following parameters:
	 * 1. eval.problem = tl.gphhucarp.NormalisedReactiveGPHHProblem
	 * 2. pop.subpop.0.species.fitness = tl.gp.fitness.NormalisedMultiObjectiveFitness
	 *
	 * @param paramFileNamePath the path to a param file including the name of the file.
	 * @param ecjParams additional parameters for ECJ.
	 */
	private static void setup(String paramFileNamePath, String... ecjParams)
	{
		state = ECJUtils.loadECJ(paramFileNamePath, ecjParams);

		Parameter p = new Parameter("eval.problem.eval-model.instances.0.samples");
		int samples = state.parameters.getInt(p, null);
		state.output.warning("Sample size: " + samples);

		p = new Parameter("eval.problem");
		state.output.warning("eval.problem: " + state.parameters.getString(p, null));

		p = new Parameter("pop.subpop.0.species.fitness");
		state.output.warning("pop.subpop.0.species.fitness: " + state.parameters.getString(p,null));
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		if (args.length < 3)
		{
			System.err.println("Invalid number of arguments. Usage: TestNormalizedFitness "
					+ " <param file> <input population file> <output folder>"
					+ " [<EJC params>...]");
			// The reason that I am not using a parameter file instead of command line arguments is
			// that first, I don't like param files; second, I want to use the same param file that
			// is used during testing without modifying it; third, the experiments already have
			// several param files that make correct management of them difficult; fifth, it is
			// easier to modify command line arguments via shell scripts.
			return;
		}

		setup(args[0], Arrays.copyOfRange(args, 2, args.length));


//		/* When a population file is evaluated, it is stored on the this folder so that other algorithms can load and use it
//		 * easier.
//		 * The value of this variable is read from commandline. This path is can be relative or absolute.
//		 */
//		Path testedPopulationFolder = Paths.get(args[2]);
//		if (!Files.exists(testedPopulationFolder) || !Files.isDirectory(testedPopulationFolder))
//			Files.createDirectories(testedPopulationFolder);

		calcRefFitness();

		Path inputPopFolder = Paths.get(args[1]);
		for (int gen = 0; gen < 1; gen++)
		{
			Path outputFile = Paths.get(args[2], "normFitnessReport." + gen + ".csv");
			PrintWriter fout = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile.toFile())));
			fout.println("loadedFitnessOnTrain,loadedFitnessOnTest,newFitnessOnTrain,normalisedFitness\n");

			Path file = Paths.get(inputPopFolder.toString(), "population.gen." + gen + ".bin");
			Population pop = PopulationUtils.loadPopulation(file.toFile());
			for (Subpopulation sub : pop.subpops)
			{
				for (int i = 0; i < sub.individuals.length; i++)
				{
					Individual ind = sub.individuals[i];
					if (!(ind instanceof GPIndividual))
					{
						System.err.println("WARNING: Found and object in the saved population file"
								+ " that is not of type GPIndividual:" + ind.getClass()
								+ " The individual is ignored.");
						continue;
					}
					evaluate(state, (GPIndividual) ind, fout);
					state.output.message("Finished evaluating the individual " + i);
				}
			}
			fout.close();
		}
	}

	static void evaluate(EvolutionState state, GPIndividual gind, PrintWriter fout)
	{
		if (!(gind instanceof TLGPIndividual))
		{
			state.output.fatal("GP individual is not of type TLGPIndividual");
			return;
		}
		TLGPIndividual tgind = (TLGPIndividual)gind;
		double loadedFitnessOnTrain = tgind.getFitnessOnTrain();
		double loadedFitnessOnTest = tgind.fitness.fitness();

		tgind.fitness = new NormalisedMultiObjectiveFitness(tgind.fitness, refFitness);
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
		NormalisedMultiObjectiveFitness nFitness = (NormalisedMultiObjectiveFitness) gind.fitness;

		double normalisedFitness = nFitness.fitness();
		double newFitnessOnTrain = nFitness.originalFitness();
		fout.println(loadedFitnessOnTrain + "," + loadedFitnessOnTest + "," + newFitnessOnTrain + "," + normalisedFitness);
		fout.flush();
		System.out.println(loadedFitnessOnTrain + "," + loadedFitnessOnTest + "," + newFitnessOnTrain + "," + normalisedFitness + "\n");
	}

	static double[] refFitness;

	static private void calcRefFitness()
	{
		PathScanning5Policy psPolicy = new PathScanning5Policy(new SimpleTieBreaker());
		MultiObjectiveFitness psFitness = FitnessUtils.create(1);
		((ReactiveGPHHProblem)(state.evaluator.p_problem)).getEvaluationModel().evaluate(psPolicy, null, psFitness, state);
		refFitness = psFitness.objectives.clone();
	}

}


