package tutorial7.gp;

import ec.simple.SimpleEvolutionState;

public class CFCEvolutionState extends SimpleEvolutionState
{
	private static final long serialVersionUID = 1L;

	public void startFresh()
	{
		output.message("Setting up");
		setup(this,null);  // a garbage Parameter

		// POPULATION INITIALIZATION
		output.message("Initializing Generation 0");
		statistics.preInitializationStatistics(this);
		population = initializer.initialPopulation(this, 0); // unthreaded
		evaluator.evaluatePopulation(this); // This is the only addition to the original file.
		statistics.postInitializationStatistics(this);

		// Compute generations from evaluations if necessary
		if (numEvaluations > UNDEFINED)
		{
			// compute a generation's number of individuals
			int generationSize = 0;
			for (int sub=0; sub < population.subpops.length; sub++)
			{
				generationSize += population.subpops[sub].individuals.length;  // so our sum total 'generationSize' will be the initial total number of individuals
			}

			if (numEvaluations < generationSize)
			{
				numEvaluations = generationSize;
				numGenerations = 1;
				output.warning("Using evaluations, but evaluations is less than the initial total population size (" + generationSize + ").  Setting to the populatiion size.");
			}
			else
			{
				if (numEvaluations % generationSize != 0)
					output.warning("Using evaluations, but initial total population size does not divide evenly into it.  Modifying evaluations to a smaller value ("
							+ ((numEvaluations / generationSize) * generationSize) +") which divides evenly.");  // note integer division
				numGenerations = (int)(numEvaluations / generationSize);  // note integer division
				numEvaluations = numGenerations * generationSize;
			}
			output.message("Generations will be " + numGenerations);
		}

		// INITIALIZE CONTACTS -- done after initialization to allow
		// a hook for the user to do things in Initializer before
		// an attempt is made to connect to island models etc.
		exchanger.initializeContacts(this);
		evaluator.initializeContacts(this);
	}
}
