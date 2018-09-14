package sandbox.tutorial6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.gp.koza.KozaFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

public class SingleValuedRegression extends GPProblem implements SimpleProblemForm 
{
	private static final long serialVersionUID = 1L;

	public static final String P_DATA = "data";
	
	public List<Double[]> mDataSet = null; 
	
	public static final String DATASET_NAME = "cancer.data";

	private static Function<String, Double[]> mapToItem = (line) -> {

		  String[] p = line.split(",");// a CSV has comma separated lines
		  ArrayList<Double> retvals = new ArrayList<>();
		  for(int i = 0; i < p.length - 1; i++) // i = 1 because the first row of the cancer dataset is ID number
		  {
			  try
			  {
				  retvals.add(Double.parseDouble(p[i+1].trim()));
			  }
			  catch(NumberFormatException e)
			  {
				  retvals.add(Double.valueOf((-1)));
			  }
		  }

		  Double[] retval = new Double[retvals.size()];
		  return retvals.toArray(retval);

		};
	
	private static List<Double[]> loadDataSet(EvolutionState state, String inputFilePath) {

	    List<Double[]> inputList = new ArrayList<Double[]>();

	    try
	    {
	      inputList = Files.lines(Paths.get(inputFilePath)).map(mapToItem).collect(Collectors.toList());
	    } catch (IOException e) 
	    {
	    	state.output.fatal("Failed to load the data file: " + inputFilePath);
	    	return null;
	    }

	    return inputList;
	}
	
	@Override
	public void setup(EvolutionState state, Parameter base) 
	{
		super.setup(state, base);
		
		if(!(input instanceof VectorData))
		{
			state.output.fatal("GPData class must subclass from" + VectorData.class, base.push(P_DATA), null);
		}
		
		mDataSet = loadDataSet(state, DATASET_NAME);
	}
	
	@Override
	public Object clone() 
	{
		SingleValuedRegression retval = (SingleValuedRegression)super.clone();
		retval.mDataSet = new ArrayList<Double[]>();
		for(Double[] row : mDataSet)
		{
			Double[] newRow = new Double[row.length];
			newRow = Arrays.copyOf(row, row.length);
			retval.mDataSet.add(newRow);
		}
		return retval; 
	}
	
	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum) 
	{
		if(ind.evaluated)
			return; 
		
		VectorData input = (VectorData)this.input;
		int hits = 0; 
		int numCorrect = 0; 
		for(Double[] row : mDataSet)
		{
			input.resetVector(row);
//			input.mDataClass = -1;
			((GPIndividual)ind).trees[0].child.eval(state, threadnum, input, stack, (GPIndividual)ind, this);
			if(input.mDataClass == input.vector[input.vector.length - 1])
				numCorrect++; 
		}
		state.output.warning(Double.toString(numCorrect));
		
		KozaFitness f = ((KozaFitness)ind.fitness);
		f.hits = hits; 
		f.setStandardizedFitness(state, mDataSet.size() - numCorrect); // Ideal fitness is zero so if data items are labeled correctly, will get this ideal value. 
		ind.evaluated = true; 
	}

}
