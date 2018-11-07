package tl.gphhucarp;

import ec.EvolutionState;
import ec.Individual;
import tl.gp.KnowledgeableProblemForm;

public class ReactiveGPHHProblem extends gphhucarp.gp.ReactiveGPHHProblem
								 implements KnowledgeableProblemForm
{
	private static final long serialVersionUID = 1L;

	private static int evalCount = 0;

	@Override
	public int getEvalCount()
	{
		return evalCount;
	}

	@Override
	public void evaluate(EvolutionState state, Individual indi, int subpopulation, int threadnum)
	{
		evalCount++;
		super.evaluate(state, indi, subpopulation, threadnum);
	}
}
