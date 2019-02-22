package gphhucarp.gp;

import java.util.ArrayList;
import java.util.Comparator;

import ec.*;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.*;
import tl.TLLogger;
import tl.gp.FCFStatistics;
import tl.gp.PopulationWriter;
import tl.knowledge.codefragment.simple.FrequentCodeFragmentKB;
import tl.knowledge.driftdetection.BaseDriftDetector;
import tl.knowledge.driftdetection.PageHinkley;
import tl.knowledge.driftdetection.SimpleDriftDetector;

public class KnowledgeableState extends GPHHEvolutionState implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Change detection method to use. Acceptable values are:
	 * - none
	 * - simple
	 * - page-hinkley
	 */
	public static final String P_CHANGE_DETECTION = "change-detction";

	/**
	 * A parameter for change detection algorithms that indicates the number of instances that the
	 * algorithms should skip until they start monitoring the stream of data for drifts. The default
	 * value for this parameter is 10.
	 */
	public static final String P_NUM_INSTANCES = "num-instance";


	/**
	 * Percentage of the population to be replaced with random immigrants. The value of this
	 * parameter needs to be in [0, 1].
	 */
	public static final String P_IMMIGRANT_PERCENT = "immigrant-percent";
	private double immigrantPercent;

	/**
	 * A parameter for the Page-Hinkley algorthim's lambda. The default value for this paramter is
	 * 10.
	 */
	public static final String P_PH_LAMBDA = "lambda";

	private BaseDriftDetector driftDetector = null;

	FrequentCodeFragmentKB knowledgeBase = null;

	private int logID;

	void setupDriftDetection(EvolutionState state, Parameter base)
	{
		Parameter changeDetectParam = base.push(P_CHANGE_DETECTION);
		String changeDetection = state.parameters.getString(changeDetectParam, null);
		Parameter numInstancesParam = changeDetectParam.push(P_NUM_INSTANCES);
		int numInstances = parameters.getIntWithDefault(numInstancesParam, null, 10);
		Parameter lambdaParam = changeDetectParam.push(P_PH_LAMBDA);
		int lambda = parameters.getIntWithDefault(lambdaParam, null, 10);

		if(changeDetection == null)
			changeDetection = "none";

		changeDetection = changeDetection.trim().toLowerCase();
		switch(changeDetection)
		{
		case "none":
			driftDetector = null;
			break;
		case "simple":
			driftDetector = new SimpleDriftDetector(numInstances);
			break;
		case "page-hinkley":
			driftDetector = new PageHinkley(numInstances, 0.005, lambda, 1 - 0.0001);
			break;
		default:
			throw new RuntimeException("Drift detection method not recognized or not supported: "
									   + changeDetection);
		}

		output.warning("KnowledgeableState loaded. Change detection: " + changeDetection
				+ ", numInstances: " + numInstances);
//				+ ", immigrantPercent: " + immigrantPercent);
	}

	void setupImmigrants(EvolutionState state, Parameter base)
	{
		Parameter immigPercent = base.push(P_IMMIGRANT_PERCENT);
		immigrantPercent = parameters.getDouble(immigPercent, null);
	}

	public static final String P_NICHE_RADIUS = "niche-radius";
	private double nicheRadius;
	void setupNiching(EvolutionState state, Parameter base)
	{
		Parameter p = base.push(P_NICHE_RADIUS);
		nicheRadius = state.parameters.getDouble(p, null);
		output.warning("Niche radius: " + nicheRadius);
	}

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(this, base);

		base = new Parameter("knowledge-state");

		logID = setupLogger(this, base);

		if(!(statistics instanceof FCFStatistics))
			throw new RuntimeException("KnowledgeableState needs an instance of FCStatistics "
					+ "as its statistics handler.");

		setupNiching(state, base);
	}

	void handleChange()
	{
		GPIndividual ind = (GPIndividual) bestIndi(0);
		double fitness = ind.fitness.fitness();

		driftDetector.add_element(fitness);
		if(driftDetector.detected_change())
		{
			System.out.println("Change detected");
			Individual[] inds = population.subpops[0].individuals;
			PopulationWriter.sort(inds);

			for(int i = 0; i < inds.length * immigrantPercent; i++)
			{
				inds[inds.length - i - 1] = population.subpops[0].species.newIndividual(this, 0);
			}
		}
	}

	private void sortNiche(ArrayList<GPIndividual> niche)
	{
		Comparator<GPIndividual> com = (a, b) -> {
			int fitnessCompare = Double.compare(a.fitness.fitness(), b.fitness.fitness());
			if(fitnessCompare == 0)
				return Long.compare(a.trees[0].child.depth(), b.trees[0].child.depth());
			return fitnessCompare;
		};
		niche.sort(com);
	}

	void applyNiching()
	{
		double radius = nicheRadius;
		int capacity = 1;

		Individual[] inds = (Individual[]) population.subpops[0].individuals;
		PopulationWriter.sort(inds);

		GPIndividual center = (GPIndividual)inds[0];
		ArrayList<GPIndividual> niche = new ArrayList<>();;
		niche.add(center);
		for(int i = 1; i < inds.length; i++)
		{
			if(Math.abs(center.fitness.fitness() - inds[i].fitness.fitness()) < radius)
			{
				niche.add((GPIndividual)inds[i]);
			}
			else
			{
				sortNiche(niche);
				log(this, logID, "Niche formed. Center: ",
						niche.get(0).trees[0].child.makeCTree(true, true, true),
						"fitness: " + niche.get(0).fitness.fitness(), "\n");
				for(int j = capacity; j < niche.size(); j++)
				{
					log(this, logID, "Discarded from niche: ",
							niche.get(j).trees[0].child.makeCTree(true, true, true),
							"fitness: " + niche.get(j).fitness.fitness(), "\n");
					((MultiObjectiveFitness)niche.get(j).fitness).objectives[0] = Double.MAX_VALUE;
				}
				niche.clear();
				niche.add((GPIndividual)inds[i]);
			}
		}
	}

    @Override
	public int evolve() {

	    if (generation > 0)
	        output.message("Generation " + generation);

	    // EVALUATION
	    statistics.preEvaluationStatistics(this);
	    evaluator.evaluatePopulation(this);
	    statistics.postEvaluationStatistics(this);

	    applyNiching();

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
	    if (generation == numGenerations-1) {
	        return R_FAILURE;
		}

	    // PRE-BREEDING EXCHANGING
	    statistics.prePreBreedingExchangeStatistics(this);
	    population = exchanger.preBreedingExchangePopulation(this);
	    statistics.postPreBreedingExchangeStatistics(this);

	    String exchangerWantsToShutdown = exchanger.runComplete(this);
	    if (exchangerWantsToShutdown!=null)
	        {
	        output.message(exchangerWantsToShutdown);
	        /*
	         * Don't really know what to return here.  The only place I could
	         * find where runComplete ever returns non-null is
	         * IslandExchange.  However, that can return non-null whether or
	         * not the ideal individual was found (for example, if there was
	         * a communication error with the server).
	         *
	         * Since the original version of this code didn't care, and the
	         * result was initialized to R_SUCCESS before the while loop, I'm
	         * just going to return R_SUCCESS here.
	         */

	        return R_SUCCESS;
	        }

	    // BREEDING
	    statistics.preBreedingStatistics(this);

	    population = breeder.breedPopulation(this);

	    // POST-BREEDING EXCHANGING
	    statistics.postBreedingStatistics(this);

	    // POST-BREEDING EXCHANGING
	    statistics.prePostBreedingExchangeStatistics(this);
	    population = exchanger.postBreedingExchangePopulation(this);
	    statistics.postPostBreedingExchangeStatistics(this);

	    // Generate new instances if needed
		if (rotateEvalModel) {
			ReactiveGPHHProblem problem = (ReactiveGPHHProblem)evaluator.p_problem;
			problem.rotateEvaluationModel();
		}


	    // INCREMENT GENERATION AND CHECKPOINT
	    generation++;
	    if (checkpoint && generation%checkpointModulo == 0)
	        {
	        output.message("Checkpointing");
	        statistics.preCheckpointStatistics(this);
	        Checkpoint.setCheckpoint(this);
	        statistics.postCheckpointStatistics(this);
	        }

	    return R_NOTDONE;
	}
}
