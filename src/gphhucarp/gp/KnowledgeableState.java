package gphhucarp.gp;

import ec.*;
import ec.gp.GPIndividual;
import ec.util.*;
import tl.gp.FCFStatistics;
import tl.gp.SimplifyingFrequentCodeFragmentBuilder;
import tl.knowledge.codefragment.simple.FrequentCodeFragmentKB;
import tl.knowledge.codefragment.simple.SimplifyingFrequentCodeFragmentKB;
import tl.knowledge.driftdetection.BaseDriftDetector;
import tl.knowledge.driftdetection.PageHinkley;
import tl.knowledge.driftdetection.SimpleDetector;

public class KnowledgeableState extends GPHHEvolutionState
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
	 * A parameter for the Page-Hinkley algorthim's lambda. The default value for this paramter is
	 * 10.
	 */
	public static final String P_PH_LAMBDA = "lambda";

	private BaseDriftDetector driftDetector = null;

	FrequentCodeFragmentKB knowledgeBase = null;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(this, base);

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
			driftDetector = new SimpleDetector(numInstances);
			break;
		case "page-hinkley":
			driftDetector = new PageHinkley(numInstances, 0.005, lambda, 1 - 0.0001);
			break;
		default:
			throw new RuntimeException("Drift detection method not recognized or not supported: "
									   + changeDetection);
		}

		if(!(statistics instanceof FCFStatistics))
			throw new RuntimeException("KnowledgeableState needs an instance of FCStatistics "
					+ "as its statistics handler.");
	}

	private void handleChange()
	{
		SimplifyingFrequentCodeFragmentKB base = SimplifyingFrequentCodeFragmentBuilder.getKnowledgeBase();
		if(base == null)
			output.fatal("Knowledgebase is empty");

		GPIndividual ind = (GPIndividual) bestIndi(0);
		double fitness = ind.fitness.fitness();
		driftDetector.add_element(fitness);
		if(driftDetector.detected_change())
		{

		}
//		else
//			base.extractFrom(ind, method);
	}

    @Override
	public int evolve() {

	    if (generation > 0)
	        output.message("Generation " + generation);

	    // EVALUATION
	    statistics.preEvaluationStatistics(this);
	    evaluator.evaluatePopulation(this);
	    statistics.postEvaluationStatistics(this);


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
