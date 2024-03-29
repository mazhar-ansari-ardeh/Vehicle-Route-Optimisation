package tl.gp;

import ec.*;
import ec.breed.BreedDefaults;
import ec.gp.GPNode;
import ec.gp.koza.CrossoverPipeline;
import ec.gp.koza.MutationPipeline;
import ec.util.Parameter;
import tl.TLLogger;

import java.util.Arrays;

/**
 * This class implements the idea of hyper-mutation. In this idea, the scenario change is treated as an environment
 * change. A typical approach in the literature is to handle the environment change with an increased mutation rate.
 * In this implementation, it is assumed that the mutation rate is initially set to a large rate and the algorithm
 * will decrease it gradually over the course of the evolution until it reaches a threshold mutation rate after which
 * it will remain fixed. The initial mutation rate is set with the utilised mutation pipeline and is not set with class.
 * This class should be used alongside an initializer that transfers knowledge such as the FullTree method.
*/
public class HyperMutationMultiBreedingPipeline extends BreedingPipeline implements TLLogger<GPNode>
{
	/**
	 * The last generation in which the probability updated
	 */
	int lastGenProbUpdated = -1;

	int mutationPipelineIndex = 1;

	int xoverPipelineIndex = -1;

	public final String P_MIN_THRESHOLD = "min-threshold";
	double minThreshold;

	/**
	 * The adaptation rate of the hypermutation. This is the rate by which the mutation probability is reduced after
	 * each generation. This value should be in (0, 1]. A value of one indicates that the mutation rate should not
	 * decrease at all.
	 */
	public final String P_ADAPT_RATE = "adapt-rate";
	private double adaptRate;

	/**
	 * The strategy for updating the mutation rate. Acceptable values are:
	 *  - exp: this maps to the expUpdate method
	 *  - sin: this maps to the sinUpdate method
	 *  - cos: this maps to the cosUpdate method
	 *  - pow: this maps to the powUpdate method
	 *  - sqrt: this maps to the sqrtUpdate method
	 *  - para: this maps to the parabolicUpdate method
	 */
	public final String P_ADAPT_STRATEGY = "adapt-strategy";
	ProbabilityUpdateStrategy probabilityUpdate = this::expUpdate;

	private double[] actualProbabilities;
	private double initialProb;

	public static final String P_GEN_MAX = "generate-max";
	public static final String P_MULTIBREED = "multibreed";

	public int maxGeneratable;
	public boolean generateMax;

	private int knowledgeSuccessLogID;

	public Parameter defaultBase()
	{
		return BreedDefaults.base().push(P_MULTIBREED);
	}

	public int numSources()
	{
		return DYNAMIC_SOURCES;
	}

	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);

		Parameter def = defaultBase();

		knowledgeSuccessLogID = setupLogger(state, base, true);

		String strategy = state.parameters.getString(base.push(P_ADAPT_STRATEGY), null);
		if(strategy == null)
		{
			// Backward compatibility
			strategy = "exp";
			log(state, knowledgeSuccessLogID, true, "Mutation probability update strategy not given. Default selected: "
					+ strategy);
		}
		switch (strategy.toLowerCase())
		{
			case "pow":
				probabilityUpdate = this::powerUpdate;
				break;
			case "exp":
				probabilityUpdate = this::expUpdate;
				break;
			case "cos":
				probabilityUpdate = this::cosUpdate;
				break;
			case "sin":
				probabilityUpdate = this::sinUpdate;
				break;
			case "sqrt":
				probabilityUpdate = this::sqrtUpdate;
				break;
			case "para":
				probabilityUpdate = this::parabolicUpdate;
				break;
			default:
				logFatal(state, knowledgeSuccessLogID, "Unknown mutation probability update strategy: ", strategy, "\n");
		}
		log(state, knowledgeSuccessLogID, true, "Mutation probability update strategy: " + strategy);

		adaptRate = state.parameters.getDouble(base.push(P_ADAPT_RATE), null);
		if(adaptRate <= 0 || adaptRate > 1)
		{
			logFatal(state, knowledgeSuccessLogID, "Invalid adaptation rate: " + adaptRate + "\n");
		}
		else
			log(state, knowledgeSuccessLogID, true, "Adaptation rate: " + adaptRate + "\n");

		minThreshold = state.parameters.getDouble(base.push(P_MIN_THRESHOLD), null);
		if(minThreshold < 0 || minThreshold > 1)
			logFatal(state,knowledgeSuccessLogID,"Invalid min threshold: " + minThreshold + "\n");
		else
			log(state,knowledgeSuccessLogID, true, "Min threshold: " + minThreshold + "\n");

		double total = 0.0;

		if (sources.length == 0)  // uh oh
			state.output.fatal("num-sources must be provided and > 0 for MultiBreedingPipeline",
					base.push(P_NUMSOURCES), def.push(P_NUMSOURCES));

		actualProbabilities = new double[sources.length];
		for (int x = 0; x < sources.length; x++)
		{
			if(sources[x] instanceof MutationPipeline)
				mutationPipelineIndex = x;
			else if(sources[x] instanceof CrossoverPipeline)
				xoverPipelineIndex = x;

			// make sure the sources are actually breeding pipelines
			if (!(sources[x] instanceof BreedingPipeline))
				state.output.error("Source #" + x + "is not a BreedingPipeline", base);
			else if (sources[x].probability < 0.0) // null checked from state.output.error above
				state.output.error("Pipe #" + x + " must have a probability >= 0.0", base);  // convenient that NO_PROBABILITY is -1...
			else total += sources[x].probability;

			actualProbabilities[x] = sources[x].probability;
		}
		initialProb = actualProbabilities[mutationPipelineIndex];

		state.output.exitIfErrors();

		// Now check for nonzero probability (we know it's positive)
		if (total == 0.0)
			state.output.warning("MultiBreedingPipeline's children have all zero probabilities -- " +
					"this will be treated as a uniform distribution.  This could be an error.", base);

		// allow all zero probabilities
		BreedingSource.setupProbabilities(sources);

		generateMax = state.parameters.getBoolean(base.push(P_GEN_MAX), def.push(P_GEN_MAX), true);
		maxGeneratable = 0;  // indicates that I don't know what it is yet.

		// declare that likelihood isn't used
		if (likelihood < 1.0)
			state.output.warning("MultiBreedingPipeline does not respond to the 'likelihood' parameter.",
					base.push(P_LIKELIHOOD), def.push(P_LIKELIHOOD));
	}

	/**
	 * Returns the max of typicalIndsProduced() of all its children
	 */
	public int typicalIndsProduced()
	{
		if (maxGeneratable == 0) // not determined yet
			maxGeneratable = maxChildProduction();
		return maxGeneratable;
	}

	@FunctionalInterface
	private interface ProbabilityUpdateStrategy
	{
		double newProb(EvolutionState state);
	}

	private double expUpdate(EvolutionState state)
	{ // p(t+1) = p(t) * (ad_rate ^ t)
		double newProb = actualProbabilities[mutationPipelineIndex] * Math.pow(adaptRate, state.generation);
		return newProb;
	}

	private double powerUpdate(EvolutionState state)
	{ // p(t) = p(0) / (1 + t/s)^c
		int t = state.generation;
		double newProb = initialProb / Math.pow(1 + t, adaptRate);
		return newProb;
	}

	private double cosUpdate(EvolutionState state)
	{ // p(t) = p(0) * cos((pi*t) / 50)
		int t = state.generation;
		double cos = Math.cos((Math.PI * t) / 50);
		double newProb = initialProb * Math.pow(cos, 2);
		return newProb;
	}

	private double sinUpdate(EvolutionState state)
	{ // p(t) = p(0) * sin((pi*t) / 50)
		int t = state.generation;
		double sin = Math.sin((Math.PI * t) / 50);
		double newProb = initialProb * Math.pow(sin, 2);
		return newProb;
	}

	private double parabolicUpdate(EvolutionState state)
	{
		int t = state.generation;
		double newProb = initialProb + Math.pow(t / 50.0, 2);
		return newProb;
	}

	private double sqrtUpdate(EvolutionState state)
	{
		int t = state.generation;
		double newProb = initialProb + Math.sqrt(t / 50.0);
		return newProb;
	}

	public int produce(final int min,
					   final int max,
					   final int start,
					   final int subpopulation,
					   final Individual[] inds,
					   final EvolutionState state,
					   final int thread)

	{
		if(lastGenProbUpdated != state.generation)
		{
			log(state, knowledgeSuccessLogID, false, "Adapting the probability. Generation: " + state.generation + "\n");
			log(state, knowledgeSuccessLogID, false, "Before adaptation: " + Arrays.toString(actualProbabilities) + "\n");
			lastGenProbUpdated = state.generation;
			double newProb = probabilityUpdate.newProb(state);
			double gap = actualProbabilities[mutationPipelineIndex] - newProb;
			if(newProb <= minThreshold)
			{
				gap = actualProbabilities[mutationPipelineIndex] - minThreshold;
				log(state,knowledgeSuccessLogID,"Threshold reached. The mutation probability will be "
						+ (actualProbabilities[mutationPipelineIndex] - gap) + "from now on.\n" );
			}

			final double maxProb = 0.95; // The reproduction probability is always set to 0.05.
			actualProbabilities[xoverPipelineIndex] += gap;
			actualProbabilities[xoverPipelineIndex] = Math.max(0, actualProbabilities[xoverPipelineIndex]); // Protect against negative values
			actualProbabilities[xoverPipelineIndex] = Math.min(maxProb, actualProbabilities[xoverPipelineIndex]); // Protect against values larger than 1

			actualProbabilities[mutationPipelineIndex] -= gap;
			actualProbabilities[mutationPipelineIndex] = Math.max(0, actualProbabilities[mutationPipelineIndex]);
			actualProbabilities[mutationPipelineIndex] = Math.min(maxProb, actualProbabilities[mutationPipelineIndex]);

			for (int x = 0; x < sources.length; x++)
			{
				sources[x].probability = actualProbabilities[x];
			}
			log(state, knowledgeSuccessLogID, false, "After adaptation: " + Arrays.toString(actualProbabilities) + "\n\n");

			BreedingSource.setupProbabilities(sources);
			if(actualProbabilities[mutationPipelineIndex] <= minThreshold)
				log(state, knowledgeSuccessLogID, "\nThreshold reached at generation: " + state.generation + "\n\n");
		}
		BreedingSource s = sources[BreedingSource.pickRandom(
				sources, state.random[thread].nextDouble())];

		log(state, knowledgeSuccessLogID,"Selected operator: " + s.getClass().getSimpleName() + "\n");
		int total;

		if (generateMax)
		{
			if (maxGeneratable == 0)
				maxGeneratable = maxChildProduction();
			int n = maxGeneratable;
			if (n < min) n = min;
			if (n > max) n = max;

			total = s.produce(
					n, n, start, subpopulation, inds, state, thread);
		} else
		{
			total = s.produce(
					min, max, start, subpopulation, inds, state, thread);
		}

		// clone if necessary
		if (s instanceof SelectionMethod)
			for (int q = start; q < total + start; q++)
				inds[q] = (Individual) (inds[q].clone());

		return total;
	}
}
