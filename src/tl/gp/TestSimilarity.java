package tl.gp;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleProblemForm;
import ec.util.*;
import gphhucarp.decisionprocess.poolfilter.ExpFeasibleNoRefillPoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import gphhucarp.gp.GPHHEvolutionState;
import tl.ecj.ECJUtils;
import tl.gp.similarity.CorrPhenoTreeSimilarityMetric;
import tl.gp.similarity.HammingPhenoTreeSimilarityMetric;
import tl.gp.similarity.PhenotypicTreeSimilarityMetric;
import tl.gp.similarity.RefRulePhenoTreeSimilarityMetric;
import tl.knowledge.surrogate.knn.FIFONoDupPhenotypicUpdatePolicy;
import tl.knowledge.surrogate.knn.KNNSurrogateFitness;

/**
 * This program is designed to read a population file, evaluate its members on test domain and test the similarity
 * between file on a new path given through commandline.
 *
 * This program does not introduce any new following ECJ parameters. <p>
 * @author mazhar
 *
 */
public class TestSimilarity
{
	static GPHHEvolutionState state = null;

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
		state = (GPHHEvolutionState) ECJUtils.loadECJ(paramFileNamePath, ecjParams);

		Parameter p = new Parameter("eval.problem.eval-model.instances.0.samples");
		int samples = state.parameters.getInt(p, null);
		if(samples < 0)
			state.output.fatal("Sample size is too small: " + samples);
		else
			state.output.warning("Sample size in EvaluateOnTest: " + samples);
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		if(args.length < 3 )
		{
			System.err.println("Invalid number of arguments. Usage: EvaluateOnTest "
					+ " <test param file> <input population file> <output file>"
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

		Path outputFile = Paths.get(args[2]);
//		Path outputFile = Paths.get("SurFitness.csv");
		PrintWriter fout = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile.toFile())));
		fout.println("FitI,FitJ,corDist,phenDist,hamDist,refDist");
		compareMetrics(pop.subpops[0].individuals, fout);

//		fout.println("Fit,corFifo");
//		compareSur(pop.subpops[0].individuals, fout);
		fout.flush();
		fout.close();

//		Path outputFile = Paths.get(testedPopulationFolder.toString(), inputPopFile.getFileName().toString());
//		PopulationUtils.savePopulation(pop, outputFile.toString());
	}

	private static void compareSur(Individual[] pop, PrintWriter fout)
	{
		Arrays.sort(pop, Comparator.comparingDouble(o -> o.fitness.fitness()));
		List<ReactiveDecisionSituation> situations = state.getInitialSituations().subList(0, Math.min(state.getInitialSituations().size(), 20));

		CorrPhenoTreeSimilarityMetric cor = new CorrPhenoTreeSimilarityMetric();
		cor.setSituations(situations);

		KNNSurrogateFitness sur = new KNNSurrogateFitness();
		sur.setMetric(cor);
		sur.setSituations(situations);
		sur.setSurrogateUpdatePolicy(new FIFONoDupPhenotypicUpdatePolicy(1024));
		sur.updateSurrogatePool(pop, "");

		for (int i = 0; i < pop.length; i++)
		{
			if(pop[i].fitness.fitness() == Double.POSITIVE_INFINITY)
				continue;
			fout.print(pop[i].fitness.fitness()+","+sur.fitness(pop[i]) + "\n");
		}
	}

	private static void compareMetrics(Individual[] pop, PrintWriter fout)
	{
		Arrays.sort(pop, Comparator.comparingDouble(o -> o.fitness.fitness()));
		List<ReactiveDecisionSituation> situations = state.getInitialSituations().subList(0, Math.min(state.getInitialSituations().size(), 20));

		CorrPhenoTreeSimilarityMetric cor = new CorrPhenoTreeSimilarityMetric();
		cor.setSituations(situations);
		PhenotypicTreeSimilarityMetric phen = new PhenotypicTreeSimilarityMetric();
		phen.setSituations(situations);
		HammingPhenoTreeSimilarityMetric ham = new HammingPhenoTreeSimilarityMetric();
		ham.setSituations(situations);
		RefRulePhenoTreeSimilarityMetric ref = new RefRulePhenoTreeSimilarityMetric(pop[0]);
		ref.setSituations(situations);

		for (int i = 0; i < pop.length; i++)
		{
			GPRoutingPolicy indi = new GPRoutingPolicy(new ExpFeasibleNoRefillPoolFilter(), ((GPIndividual) pop[i]).trees[0]);
			for (int j = i+1; j < pop.length; j++)
			{
				GPRoutingPolicy indj = new GPRoutingPolicy(new ExpFeasibleNoRefillPoolFilter(), ((GPIndividual) pop[j]).trees[0]);
				double corDist = cor.distance(indi, indj);
				double phenDist = phen.distance(indi, indj);
				double hamDistance = ham.distance(indi, indj);
				double refDistance = ref.distance(indi, indj);
				fout.print(pop[i].fitness.fitness()+",");
				fout.print(pop[j].fitness.fitness()+",");
				fout.print(corDist + "," + phenDist + "," + hamDistance + "," + refDistance + "\n");
//				fout.print(indi.getGPTree().child.makeLispTree()+",");
//				fout.println(indj.getGPTree().child.makeLispTree()+",");
			}
		}
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
				return;
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
	}

}
