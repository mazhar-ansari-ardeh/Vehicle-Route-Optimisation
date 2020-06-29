package tl.gp.fitness;

import ec.EvolutionState;
import ec.Fitness;
import ec.multiobjective.MultiObjectiveFitness;
import ec.util.Code;
import ec.util.DecodeReturn;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;

/**
 * This is a adaptor class that allows normalising fitness values with a reference fitness. This class works with
 * the {@code tl.gphhucarp.NormalisedReactiveGPHHProblem} class.
 */
public class NormalisedMultiObjectiveFitness extends MultiObjectiveFitness
{
	private double[] referenceFitness;

	public NormalisedMultiObjectiveFitness(MultiObjectiveFitness fitness, double referenceFitness)
	{
		if(fitness == null)
			throw new IllegalArgumentException("Fitness cannot be null");
		if(referenceFitness == 0)
			throw new IllegalArgumentException("Reference fitness cannot be zero");

		if(fitness.maxObjective != null)
			this.maxObjective = fitness.maxObjective.clone();
		if (fitness.minObjective != null)
			this.minObjective = fitness.minObjective.clone();
		if(fitness.maximize != null)
			this.maximize = fitness.maximize.clone();
		if(fitness.objectives == null || fitness.objectives.length == 0)
			throw new IllegalArgumentException("Objectives cannot be null or empty");
		this.objectives = fitness.objectives.clone();

		this.referenceFitness = new double[fitness.objectives.length];
		Arrays.fill(this.referenceFitness, referenceFitness);
	}

	public NormalisedMultiObjectiveFitness(MultiObjectiveFitness fitness, double[] referenceFitnesses)
	{
		if(fitness == null)
			throw new IllegalArgumentException("Fitness cannot be null");
		if(referenceFitnesses == null || referenceFitnesses.length == 0)
			throw new IllegalArgumentException("Reference fitness cannot be null or empty");

		if(fitness.maxObjective != null)
			this.maxObjective = fitness.maxObjective.clone();
		if (fitness.minObjective != null)
			this.minObjective = fitness.minObjective.clone();
		if(fitness.maximize != null)
			this.maximize = fitness.maximize.clone();
		if(fitness.objectives == null || fitness.objectives.length == 0)
			throw new IllegalArgumentException("Objectives cannot be null or empty");
		if(fitness.objectives.length != referenceFitnesses.length)
			throw new IllegalArgumentException("Length of reference fitness mismatch");
		this.objectives = fitness.objectives.clone();

		this.referenceFitness = referenceFitnesses.clone();
	}

	public NormalisedMultiObjectiveFitness(Fitness fitness, double[] referenceFitnesses)
	{
//		if( !(fitness instanceof MultiObjectiveFitness))
//			throw new IllegalArgumentException("Fitness must be of type MultiObjectiveFitness");
		this((MultiObjectiveFitness)fitness, referenceFitnesses);
	}

	// The stupid ECJ complains if this is missing.
	public NormalisedMultiObjectiveFitness()
	{
	}

	public void setReferenceFitness(double[] referenceFitness)
	{
		if(referenceFitness == null)
			throw new IllegalArgumentException("Reference fitness cannot be null");
		if(this.objectives.length != referenceFitness.length)
			throw new IllegalArgumentException("Length of the given reference fitness does not match");
		this.referenceFitness = referenceFitness.clone();
	}

	public NormalisedMultiObjectiveFitness(Fitness fitness, double referenceFitness)
	{
		this(((MultiObjectiveFitness)(fitness)), referenceFitness);
	}

	/**
	 * Gets the original fitness value that is not normalised.
	 * @return Fitness before normalisation
	 */
	public double originalFitness()
	{
		return super.fitness();
	}

	/**
	 * Returns the normalised fitness.
	 * @return Normalised fitness.
	 */
	@Override
	public double fitness()
	{
		double fit = objectives[0];
		double ref = referenceFitness[0];
		for (int x = 1; x < objectives.length; x++)
			if (fit < objectives[x])
			{
				fit = objectives[x];
				ref = referenceFitness[x];
			}
		return fit / ref;
	}

	/**
	 * Returns true if I'm better than _fitness. The rule I'm using is this: if
	 * I am better in one or more criteria, and we are equal in the others, then
	 * betterThan is true, else it is false.
	 */
	@Override
	public boolean paretoDominates(MultiObjectiveFitness other)
	{
		if(!(other instanceof NormalisedMultiObjectiveFitness))
			throw new RuntimeException("The other must be of this type.");
		NormalisedMultiObjectiveFitness fOther = (NormalisedMultiObjectiveFitness)other;
		boolean abeatsb = false;

		if(objectives == null)
			throw new RuntimeException("Objectives are null");
		if(referenceFitness == null)
			throw new RuntimeException("Reference fitnesses are null");

		if(other.objectives == null)
			throw new RuntimeException("Objectives are null");
		if(fOther.referenceFitness == null)
			throw new RuntimeException("Reference fitnesses are null");

		if (objectives.length != other.objectives.length)
			throw new RuntimeException("Attempt made to compare two multiobjective fitnesses; but they have different numbers of objectives.");

		for (int x = 0; x < objectives.length; x++)
		{
			if (maximize[x] != other.maximize[x])  // uh oh
				throw new RuntimeException(
						"Attempt made to compare two multiobjective fitnesses; but for objective #" + x +
								", one expects higher values to be better and the other expectes lower values to be better.");

			if (maximize[x])
			{
				if ((objectives[x] / referenceFitness[x]) > (other.objectives[x] / fOther.referenceFitness[x]) )
					abeatsb = true;
				else if ( (objectives[x] / referenceFitness[x]) < (other.objectives[x] / fOther.referenceFitness[x]))
					return false;
			}
			else
			{
				if ( (objectives[x] / referenceFitness[x]) < (other.objectives[x] / fOther.referenceFitness[x]))
					abeatsb = true;
				else if ((objectives[x] / referenceFitness[x]) > (other.objectives[x] / fOther.referenceFitness[x]))
					return false;
			}
		}

		return abeatsb;
	}

	/**
	 * Returns the sum of the squared difference between two Fitnesses in Objective space.
	 */
	public double sumSquaredObjectiveDistance(MultiObjectiveFitness other)
	{
		if(!(other instanceof NormalisedMultiObjectiveFitness))
			throw new RuntimeException("The other must be of this type.");
		NormalisedMultiObjectiveFitness fOther = (NormalisedMultiObjectiveFitness)other;

		double s = 0;
		for (int i = 0; i < objectives.length; i++)
		{
			double a = ( (objectives[i] / referenceFitness[i]) - (other.objectives[i] / fOther.referenceFitness[i]));
			s += a * a;
		}
		return s;
	}

	public void setToMeanOf(EvolutionState state, Fitness[] fitnesses)
	{
		throw new RuntimeException("Not supported"); // Basically because I don't know what it does.
	}

	/**
	 * Returns the Manhattan difference between two Fitnesses in Objective space.
	 */
	public double manhattanObjectiveDistance(MultiObjectiveFitness other)
	{
		if(!(other instanceof NormalisedMultiObjectiveFitness))
			throw new RuntimeException("The other must be of this type.");
		NormalisedMultiObjectiveFitness fOther = (NormalisedMultiObjectiveFitness)other;

		double s = 0;
		for (int i = 0; i < objectives.length; i++)
		{
			s += Math.abs((objectives[i] / referenceFitness[i]) - (other.objectives[i] / fOther.referenceFitness[i]));
		}
		return s;
	}

	public boolean equivalentTo(Fitness _fitness)
	{
		NormalisedMultiObjectiveFitness other = (NormalisedMultiObjectiveFitness) _fitness;
		boolean abeatsb = false;
		boolean bbeatsa = false;

		if (objectives.length != other.objectives.length)
			throw new RuntimeException("Attempt made to compare two multiobjective fitnesses; but they have different numbers of objectives.");

		for (int x = 0; x < objectives.length; x++)
		{
			if (maximize[x] != other.maximize[x])  // uh oh
				throw new RuntimeException(
						"Attempt made to compare two multiobjective fitnesses; but for objective #" + x +
								", one expects higher values to be better and the other expectes lower values to be better.");

			if (maximize[x])
			{
				if ( (objectives[x] / referenceFitness[x]) > (other.objectives[x] / other.referenceFitness[x]))
					abeatsb = true;
				if ((objectives[x] / referenceFitness[x]) < (other.objectives[x] / other.referenceFitness[x]))
					bbeatsa = true;
			}
			else
			{
				if ( (objectives[x] / referenceFitness[x]) < (other.objectives[x] / other.referenceFitness[x]))
					abeatsb = true;
				if ((objectives[x] / referenceFitness[x]) > (other.objectives[x] / other.referenceFitness[x]))
					bbeatsa = true;
			}
			if (abeatsb && bbeatsa)
				return true;
		}
		return !abeatsb && !bbeatsa;
	}

	public String fitnessToString()
	{
		StringBuilder s = new StringBuilder(FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE);
		for (int x = 0; x < objectives.length; x++)
		{
			if (x > 0)
				s.append(" ");
			s.append(Code.encode(objectives[x]));

			s.append(" ");
			s.append(Code.encode(referenceFitness[x]));
		}

		return s + FITNESS_POSTAMBLE;
	}

	public String fitnessToStringForHumans()
	{
		StringBuilder s = new StringBuilder(FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE);
		for (int x = 0; x < objectives.length; x++)
		{
			if (x > 0)
				s.append(" ");
			s.append(objectives[x]);

			s.append(" [");
			s.append(referenceFitness[x]).append("]");
		}

		return s + FITNESS_POSTAMBLE;
	}

	public void readFitness(final EvolutionState state, final LineNumberReader reader)
	{
		DecodeReturn d = Code.checkPreamble(FITNESS_PREAMBLE + MULTI_FITNESS_POSTAMBLE, state, reader);
		if(d == null)
			throw new RuntimeException("checkPreample returned null");
		for (int x = 0; x < objectives.length; x++)
		{
			Code.decode(d);
			if (d.type != DecodeReturn.T_DOUBLE)
				state.output.fatal("Reading Line " + d.lineNumber + ": " + "Bad Fitness (objectives value #" + x + ").");
			objectives[x] = d.d;

			Code.decode(d);
			if (d.type != DecodeReturn.T_DOUBLE)
				state.output.fatal("Reading Line " + d.lineNumber + ": " + "Bad Fitness (objectives value #" + x + ").");
			referenceFitness[x] = d.d;
		}
	}

	public void writeFitness(final EvolutionState state, final DataOutput dataOutput) throws IOException
	{
		dataOutput.writeInt(objectives.length);
		for (int i = 0; i < objectives.length; i++)
		{
			double objective = objectives[i];
			dataOutput.writeDouble(objective);

			double reference = referenceFitness[i];
			dataOutput.writeDouble(reference);
		}
	}

	public void readFitness(final EvolutionState state, final DataInput dataInput) throws IOException
	{
		int len = dataInput.readInt();
		if (objectives == null || objectives.length != len)
			objectives = new double[len];
		for (int x = 0; x < objectives.length; x++)
		{
			objectives[x] = dataInput.readDouble();
			referenceFitness[x] = dataInput.readDouble();
		}

		readTrials(state, dataInput);
	}
}