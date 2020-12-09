package tl.knowledge.surrogate;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import tl.gp.TLGPIndividual;

public class SuGPIndividual extends TLGPIndividual
{
	public double getSurFit()
	{
		return surFit;
	}

	public static SuGPIndividual asGPIndividual(GPNode root, double fitness, boolean evaluated)
	{
		SuGPIndividual retval = new SuGPIndividual();
		retval.trees = new GPTree[1];
		retval.trees[0] = new GPTree();
		retval.trees[0].child = root;
		root.parent =retval.trees[0];

//		MultiObjectiveFitness fitness = new MultiObjectiveFitness();
//		fitness.objectives = new double[1];
//
//		retval.fitness = fitness;
//		retval.evaluated = false;

		MultiObjectiveFitness aFitness = new MultiObjectiveFitness();
		aFitness.objectives = new double[]{fitness};
		aFitness.maximize = new boolean[]{false};
		retval.fitness = aFitness;
		retval.evaluated = evaluated;
		return retval;
	}

	public static SuGPIndividual asGPIndividual(GPNode root, double fitness)
	{
		return asGPIndividual(root, fitness, false);
	}

	public static SuGPIndividual asGPIndividual(GPIndividual ind, int tree, boolean evaluated)
	{
		GPNode root = ind.trees[tree].child;
		double fitness = ind.fitness.fitness();
		return asGPIndividual(root, fitness, evaluated);
	}

	public void setSurFit(double surFit)
	{
		this.surFit = surFit;
	}

	public long getSurtime()
	{
		return surtime;
	}

	public void setSurtime(long surtime)
	{
		this.surtime = surtime;
	}

//	public long getEvalTime()
//	{
//		return evalTime;
//	}
//
//	public void setEvalTime(long evalTime)
//	{
//		this.evalTime = evalTime;
//	}

	transient private double surFit;
	transient private long surtime;
//	transient private long evalTime;
}
