package sandbox.tutorial4;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

public class MultiValuedRegression extends GPProblem implements SimpleProblemForm 
{
	private static final long serialVersionUID = 1L;

	public static final String P_DATA = "data";
	
	public double currentX;
	
	public double currentY;
	
	@Override
	public void setup(EvolutionState state, Parameter base) 
	{
		super.setup(state, base);
		
		if(!(input instanceof DoubleData))
		{
			state.output.fatal("GPData class must subclass from" + DoubleData.class, base.push(P_DATA), null);
		}
	}
	
	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum) 
	{
		if(ind.evaluated)
			return; 
		
		DoubleData input = (DoubleData)this.input;
		int hits = 0; 
		double sum = 0;
		double expectedResult = 0;
		double result; 
		for(int y = 0; y < 10; y++)
		{
			currentX = state.random[threadnum].nextDouble();
			currentY = state.random[threadnum].nextDouble();
			expectedResult = currentX*currentX*currentY + currentX*currentY + currentY;
			((GPIndividual)ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual)ind, this);
			
			result = Math.abs(input.x - expectedResult);
			if(result < 0.01)
			{
				hits++;
			}
			sum += result; 
		}
		
		KozaFitness f = ((KozaFitness)ind.fitness);
		f.hits = hits; 
		f.setStandardizedFitness(state, sum);
		ind.evaluated = true; 
	}

}
