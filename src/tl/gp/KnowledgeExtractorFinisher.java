package tl.gp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import ec.*;
import ec.gp.*;
import ec.simple.SimpleFinisher;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import tl.knowledge.codefragment.fitted.DoubleFittedCodeFragment;

public class KnowledgeExtractorFinisher extends SimpleFinisher 
{
	private static final long serialVersionUID = 1L;

	private KnowledgeExtractionMethod extractionMethod;
	
	private String knowlegeFileName;
	
	public static final String P_KNOWLEDGE_EXTRACTION_METHOD = "tl.gp.knowledge-extractor-finisher.method";
	public static final String P_KNOWLEDGE_FILE_NAME = "tl.gp.knowledge-extractor-finisher.file-name";

	@Override
	public void setup(EvolutionState state, Parameter base) 
	{
		super.setup(state, base);

		Parameter p = new Parameter(P_KNOWLEDGE_EXTRACTION_METHOD);
		String method = state.parameters.getString(p, null);
		extractionMethod = KnowledgeExtractionMethod.parse(method);
		
		p = new Parameter(P_KNOWLEDGE_FILE_NAME);
		knowlegeFileName = state.parameters.getString(p, null);
	}

	@Override
	public void finishPopulation(EvolutionState state, int result) 
	{
		super.finishPopulation(state, result);

		FCFStatistics stats = (FCFStatistics) state.statistics;

		String fileName = stats.generatePopulationFileName(stats.bestGeneration);

		File file = new File(fileName);
		if(!file.exists())
		{
			state.output.fatal("Generation file for extracting knowledge not found: " + fileName);
			return;
		}

		// TODO: This is a very bad file name. CHANGE IT.
		String outputFileName = this.knowlegeFileName;
		state.output.warning("Extracting code fragments from the file: " + fileName 
				+ " and saving to: " + outputFileName);
		
		File outputFile = new File(outputFileName);
		if(outputFile.exists())
			outputFile.delete();
		// outputFile.createNewFile();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)); 
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFile)))
		{
		
			int nsub = ois.readInt();
			for(int i = 0; i < nsub; i++)
			{
				int nind = ois.readInt();
				for(int j = 0; j < nind; j++)
				{
					Object ind = ois.readObject();
					if(!(ind instanceof GPIndividual))
					{
						state.output.warning("WARNING: object of instance " 
											 + ind.getClass().toString() + "found in saved file");
						continue;
					}
					extractAndSave(state, oos, (GPIndividual) ind);
				}
			}
			//ois.close();
			oos.flush();
			//oos.close();
		}
		catch(IOException | ClassNotFoundException exp)
		{
			state.output.fatal("Exception occurred reading knowledge file: " + exp.toString());
		}
		state.output.warning("Finished extracting code fragments from the file: " + fileName 
				+ " and saved to: " + outputFile.getAbsolutePath());
	}
	
	private void extractAndSave(EvolutionState state, ObjectOutputStream oos, 
								GPIndividual gind) throws IOException
	{		
		ArrayList<GPNode> list = null;
		switch(extractionMethod)
		{
		case AllSubtrees:
			list = TreeSlicer.sliceAllToNodes(gind, false);
			break;
		case RootSubtree:
			list = TreeSlicer.sliceRootChildrenToNodes(gind, false);
			break;
		default: 
			state.output.fatal("Received an unknown extraction method: " + extractionMethod);
		}
		
		if(!(state.evaluator.p_problem instanceof SimpleProblemForm))
			state.output.fatal("GP problem is not an instance of SimpleProblemForm: " 
								+ state.evaluator.p_problem.getClass().toString());
		SimpleProblemForm problem = (SimpleProblemForm)state.evaluator.p_problem;
		
		for(GPNode node : list)
		{
			// Convert node to individual
			GPIndividual newind = gind.lightClone();
			newind.trees[0].child = node;
			node.parent = newind.trees[0].child; 
			
			// Get its fitness
			problem.evaluate(state, newind, 0, 0);
			node.parent = null; 
			
			// Save it to file
			DoubleFittedCodeFragment cf = new DoubleFittedCodeFragment(node,
					null, newind.fitness.fitness());
			oos.writeObject(cf);
			oos.flush();
		}
	}
}
