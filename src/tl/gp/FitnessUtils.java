package tl.gp;

import ec.multiobjective.MultiObjectiveFitness;

public class FitnessUtils
{
	public static MultiObjectiveFitness create(int numObjectives)
	{
		MultiObjectiveFitness retval = new MultiObjectiveFitness();
		retval.maximize = new boolean[numObjectives];
		retval.minObjective = new double[numObjectives];
		retval.maxObjective = new double[numObjectives];
		retval.objectives = new double[numObjectives];

		return retval;
	}
}
