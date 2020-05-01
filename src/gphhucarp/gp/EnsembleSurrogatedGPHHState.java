package gphhucarp.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Parameter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import tl.TLLogger;
import tl.gp.niching.SimpleNichingAlgorithm;
import tl.gp.similarity.*;
import tl.knowledge.surrogate.knn.*;

import java.io.File;
import java.util.*;

public class EnsembleSurrogatedGPHHState extends GPHHEvolutionState implements TLLogger<GPNode>
{
	public EnsembleSurrogateFitness surFitness;

	@Override
	List<ReactiveDecisionSituation> getAllSeenSituations()
	{
		return super.getAllSeenSituations();
	}

	private int surLogID;
	private int interimPopLogID;
	private int popLogID;

	private String surLogPath;

	private static final String P_BASE = "ensemble-surrogate-state";

	private static final String P_SURR_LOG_PATH = "surr-log-path";

	private static final String P_EVAL_SURPOOL_ON_INIT = "eval-surpool-on-init";
	private boolean evaluateSurOnInit = true;

	/**
	 * The distance metric that KNN uses. Acceptable values are (case insensitive):
	 *  - phenotypic
	 *  - corrphenotypic
	 *  - hamphenotypic
	 * Some updating policies, such as CorrEntropy or Entropy, may have their own metric and will override this
	 * parameter.
	 */
	private static final String P_KNN_DISTANCE_METRIC = "knn-distance-metric";



	/**
	 * Number of decision-making situations to consider for phenotypic characterisation.
	 */
	public final int DMS_SIZE = 30;

	private void setupEnsemnle(EvolutionState state, Parameter surBase)
	{
		evaluateSurOnInit = parameters.getBoolean(surBase.push(P_EVAL_SURPOOL_ON_INIT), null, true);
		state.output.warning("Evaluate surrogate pool on initialisation: " + evaluateSurOnInit);

		surLogPath = state.parameters.getString(surBase.push(P_SURR_LOG_PATH), null);
		if(surLogPath == null)
		{
			state.output.fatal("Surrogate log path cannot be null");
			return;
		}
		state.output.warning("Surrogate log path: " + surLogPath);

		surLogID = setupLogger(this, new File(surLogPath, "surr/SurrogatePool.0.csv").getAbsolutePath());

		SituationBasedTreeSimilarityMetric metrics = null;
		String metricParam = state.parameters.getString(surBase.push(P_KNN_DISTANCE_METRIC), null);
		if(metricParam.equalsIgnoreCase("CorrPhenotypic"))
			metrics = new CorrPhenoTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Phenotypic"))
			metrics = new PhenotypicTreeSimilarityMetric();
		else if(metricParam.equalsIgnoreCase("Hamming"))
			metrics = new HammingPhenoTreeSimilarityMetric();
		else
			state.output.fatal("Unknown distance metric");

		KNNSurrogateFitness surr1 = new KNNSurrogateFitness();
		surr1.setSurrogateUpdatePolicy(new AddOncePhenotypicUpdatePolicy());
		surr1.setMetric(metrics);

		KNNSurrogateFitness surr2 = new KNNSurrogateFitness();
		surr2.setSurrogateUpdatePolicy(new ResetUpdatePolicy());
		surr2.setMetric(metrics);

		surFitness = new EnsembleSurrogateFitness(surr1, surr2);
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		Parameter surBase = new Parameter(P_BASE);
		setupEnsemnle(state, surBase);
		super.setup(this, base);
		surFitness.setFilter(((ReactiveGPHHProblem)(this.evaluator.p_problem)).poolFilter);

		interimPopLogID = setupLogger(this, new File(surLogPath, "pop/InterimPop.0.csv").getAbsolutePath());
		log(this, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");

		popLogID = setupLogger(this, new File(surLogPath, "pop/Pop.0.csv").getAbsolutePath());
		log(this, popLogID, "SurrogateTime,EvaluationTime,SurrogateFitness,Fitness,Tree\n");
	}

	private ArrayList<Population> breedMore(int howMany)
	{
		ArrayList<Population> retVal = new ArrayList<>(howMany);
		while (howMany-- > 0)
		{
			Population p = breeder.breedPopulation((this));
//            for(Individual ind : p.subpops[0].individuals)
//                System.out.println(((GPIndividual)ind).trees[0].child.makeLispTree());
			Population ret = (Population) p.emptyClone();
			for(int i = 0; i < p.subpops.length; i++)
				ret.subpops[i].individuals = p.subpops[0].individuals;
			retVal.add(ret);
		}

		return retVal;
	}

	private ArrayList<Population> temporaryPop;


	private void evaluate()
	{
		statistics.preEvaluationStatistics(this);

		if(generation == 0) // This is transferred from the past
		{
			if(evaluateSurOnInit)
				surFitness.rerevaluate(ind ->
				{
					((ReactiveGPHHProblem)this.evaluator.p_problem).evaluate(this, ind, 0, 0);
					return ind.fitness.fitness();
				});

			temporaryPop = new ArrayList<>();
			temporaryPop.add(population);
			temporaryPop.addAll(breedMore(2));
		}
		if(surFitness.isKNNPoolEmpty())
			log(this, surLogID, "Warning: Surrogate pool is empty.\n");

		Vector<Individual> allInds = new Vector<>();

		for(Population pop : temporaryPop)
		{
			for(Individual ind : pop.subpops[0].individuals)
			{
				ind.surtime = System.currentTimeMillis();
				double fit = surFitness.fitness(ind);
				ind.surtime = System.currentTimeMillis() - ind.surtime;
				((MultiObjectiveFitness)ind.fitness).objectives[0] = fit;
				ind.surFit = fit;
				allInds.add(ind);
			}
		}

		allInds.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
		SimpleNichingAlgorithm.clearPopulation(allInds, surFitness.getSituations(), 0.0, 1);
		allInds.sort((o1, o2) -> {
			int c = Double.compare(o1.fitness.fitness(), o2.fitness.fitness());
			if(c == 0)
				return Integer.compare(((GPIndividual)o1).trees[0].child.depth(), ((GPIndividual)o2).trees[0].child.depth());
			return c;
		});
		allInds.forEach(i -> log(this, interimPopLogID, i.surFit + "," + i.fitness.fitness()
				+ "," + ((GPIndividual)i).trees[0].child.makeLispTree() + "\n"));

		Population pop = temporaryPop.get(0);
		pop.subpops[0].individuals = allInds.subList(0, population.subpops[0].individuals.length).toArray(new Individual[]{});
		population = pop;

		log(this, surLogID, surFitness.logSurrogate());

		evaluator.evaluatePopulation(this);
		for(Individual ind : population.subpops[0].individuals)
		{
			log(this, popLogID, ind.surtime + "," + ind.evalTime + "," + ind.surFit + "," + ind.fitness.fitness() + ","
					+ ((GPIndividual)ind).trees[0].child.makeLispTree() + "\n");
		}

		surFitness.setSituations(getAllSeenSituations().subList(0, DMS_SIZE));
		surFitness.updateSurrogatePool(population.subpops[0].individuals, ("t:gen_" + generation));
		if(generation > 0)
			surFitness.updateWeights(population.subpops[0].individuals);

		statistics.postEvaluationStatistics(this);
	}

	protected void breed()
	{
		statistics.preBreedingStatistics(this);

		temporaryPop = breedMore(3);

		// POST-BREEDING EXCHANGING
		statistics.postBreedingStatistics(this);
	}


	@Override
	public int evolve() {

		if (generation > 0)
		{
//            breed();
			output.message("Generation " + generation);
		}

//        List<ReactiveDecisionSituation> situations = initialSituations;
//
//        PhenoCharacterisation ph = new PhenoCharacterisation(situations);
//        ph.cha(new GPRoutingPolicy(((GPIndividual)(population.subpops[0].individuals[0])).trees[0]));

		evaluate();

		updateLoggers();

		finish = util.Timer.getCpuTime();
		duration = 1.0 * (finish - start) / 1000000000;

		writeToStatFile();

		start = util.Timer.getCpuTime();

		// SHOULD WE QUIT?
		if (evaluator.runComplete(this) && quitOnRunComplete) {
			output.message("Found Ideal Individual");
			return R_SUCCESS;
		}

		// SHOULD WE QUIT?
		if (generation == numGenerations-1) return R_FAILURE;

		if (exchangePopulationPreBreeding()) return R_SUCCESS;

		// BREEDING
		breed();

		// POST-BREEDING EXCHANGING
		exchangePopulationPostBreeding();

		// Generate new instances if needed
		rotateEvalModel();

		// INCREMENT GENERATION AND CHECKPOINT
		generation++;
		doCheckpoit();

		return R_NOTDONE;
	}

	private void updateLoggers()
	{
		closeLogger(this, surLogID);
		surLogID = setupLogger(this, new File(surLogPath, "surr/SurrogatePool." + (generation + 1) + ".csv").getAbsolutePath());

		closeLogger(this, popLogID);
		popLogID = setupLogger(this, new File(surLogPath, "pop/Pop." + (generation + 1) + ".csv").getAbsolutePath());
		log(this, popLogID, "SurrogateTime,EvaluationTime,SurrogateFitness,Fitness,Tree\n");

		closeLogger(this, interimPopLogID);
		interimPopLogID = setupLogger(this, new File(surLogPath, "pop/InterimPop." + (generation + 1) + ".csv").getAbsolutePath());
		log(this, interimPopLogID, "SurrogateFitness,SurrogateFitnessAfterClearing,Individual\n");
	}
}
