package tl.gphhucarp;


import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.gp.GPIndividual;
import ec.multiobjective.MultiObjectiveFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.routingpolicy.PathScanning5Policy;
import gphhucarp.decisionprocess.tiebreaker.SimpleTieBreaker;
import gphhucarp.gp.ReactiveGPHHProblem;
import tl.ecj.ECJUtils;
import tl.gp.FitnessUtils;
import tl.gp.PopulationUtils;
import tl.gp.TLGPIndividual;
import tl.gp.fitness.NormalisedMultiObjectiveFitness;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.SituationBasedTreeSimilarityMetric;
import tl.gphhucarp.dms.DMSSaver;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * This program is designed to read population files that are tested on a test domain, load their individuals and
 * evaluate their members on train domain with the normalised fitness class and then, compare the performance against
 * the test performance.
 *
 * This program does not introduce any new ECJ parameters. <p>
 * @author mazhar
 *
 */
public class TestDomainSimilarity
{
	static EvolutionState state = null;

	static String dataset;

	static int vehicles;
	private static PoolFilter filter;

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

		dataset = state.parameters.getString(new Parameter("eval.problem.eval-model.instances.0.file"), null);
		state.output.warning("Dataset: " + dataset);
		dataset = dataset.split("/")[1].split("\\.")[0];
		vehicles = state.parameters.getInt(new Parameter("eval.problem.eval-model.instances.0.vehicles"), null);

		p = new Parameter("eval.problem.pool-filter");
		filter = (PoolFilter)(state.parameters.getInstanceForParameter(p, null, PoolFilter.class));

		state.output.warning("Vehicles: " + vehicles);
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

		Path inputPopFolder = Paths.get("./KnowledgeSource/gdb1.vs5-gen_50/3/TestedPopulation/");
		int gen = 0;

		Path outputFile = Paths.get(args[2], "T" + dataset + "v" + vehicles + ".csv");
		PrintWriter fout = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile.toFile())));
		fout.print("FitnessOnTrain,NormalisedFitness\n");

		Path file = Paths.get(inputPopFolder.toString(), "population.gen." + gen + ".bin");
		Population pop = PopulationUtils.loadPopulation(file.toFile());
		SituationBasedTreeSimilarityMetric metric = new PhenotypicTreeSimilarityMetric();
		metric.setSituations(((DMSSaver)state).getInitialSituations().subList(0, 20));
		List<Individual> inds = PopulationUtils.loadPopulations(state, inputPopFolder.toString(), 48, 49, filter,
				metric, 0, 1, null, -1, false);
		for (Subpopulation sub : pop.subpops)
		{
			for (int i = 0; i < inds.size(); i++)
			{
				Individual ind = inds.get(i);
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

	static void evaluate(EvolutionState state, GPIndividual gind, PrintWriter fout)
	{
		if (!(gind instanceof TLGPIndividual))
		{
			state.output.fatal("GP individual is not of type TLGPIndividual");
			return;
		}
		TLGPIndividual tgind = (TLGPIndividual)gind;

		tgind.fitness = new NormalisedMultiObjectiveFitness(tgind.fitness, refFitness);
		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, gind, 0, 0);
		NormalisedMultiObjectiveFitness nFitness = (NormalisedMultiObjectiveFitness) gind.fitness;

		double normalisedFitness = nFitness.fitness();
		double newFitnessOnTrain = nFitness.originalFitness();
		fout.println(newFitnessOnTrain + "," + normalisedFitness); // + "," + tgind.trees[0].child.makeLispTree());
		fout.flush();
		System.out.println(newFitnessOnTrain + "," + normalisedFitness + "\n");
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


