package tl.gp;

import ec.gp.GPIndividual;
import ec.simple.SimpleProblemForm;
import gphhucarp.gp.CalcPriorityProblem;
import gphhucarp.gp.terminal.FeatureGPNode;
import tl.knowledge.codefragment.CodeFragmentKI;

public class CFTerminal extends FeatureGPNode
{
	private static final long serialVersionUID = 1L;

	CodeFragmentKI cf;
	SimpleProblemForm problem;
	public CFTerminal(CodeFragmentKI cf, SimpleProblemForm problem)
	{
		this.cf = cf;
		this.name = "CFNode";
		this.problem = problem;
	}

	@Override
	public double value(CalcPriorityProblem calcPriorityProblem)
	{
		GPIndividual ind = cf.getAsIndividual();
		problem.evaluate(null, ind, 0, 0);
//		if(fitness != null)
//			return fitness;

//		GPIndividual ind = cf.getAsIndividual();
//		((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, 0, 0);
//		fitness = ind.fitness.fitness();
//		// calcPriorityProblem.evaluate(state, ind, 0, 0);
		return ind.fitness.fitness();
	}
}