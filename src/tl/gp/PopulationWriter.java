package tl.gp;

import java.io.*;
import ec.*;

public class PopulationWriter implements AutoCloseable
{
	private boolean isSaving; 
	private File file;
	ObjectOutputStream output = null;
	
	public PopulationWriter(String pathname, boolean save)
	{
		this.isSaving = save;
		file = new File(pathname);
	}
	
	public void prepareForWriting(Population population, Subpopulation sub) throws IOException
	{
		if(!isSaving)
			throw new IOException("This object is not initialized for saving objects.");
		if(output == null)
		{
			output = new ObjectOutputStream(new FileOutputStream(file));
			output.writeInt(population.subpops.length);
		}
		
		output.writeInt(sub.individuals.length);
	}
	
	public void write(Individual ind) throws IOException
	{
		if(!isSaving)
			throw new IOException("This object is not initialized for saving objects.");
		
		output.writeObject(ind);
	}
	
	public static void savePopulation(Population pop, String fileName) 
			throws FileNotFoundException, IOException
	{	
		File file = new File(fileName);
		if(file.exists())
			file.delete();
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
		{
			int nSubPops = pop.subpops.length;
			oos.writeInt(nSubPops);
			for(Subpopulation subpop : pop.subpops)
			{
				int nInds = subpop.individuals.length;
				oos.writeInt(nInds);
				for(Individual ind : subpop.individuals)
				{
					oos.writeObject(ind);
				}
			}
		}
	}
	
	public static Population loadPopulation(String fileName) 
			throws FileNotFoundException, IOException, ClassNotFoundException, InvalidObjectException
	{
		File file = new File(fileName);
		Population retval = new Population();
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)))
		{
			int numSub = ois.readInt();
			retval.subpops = new Subpopulation[numSub];
			for(int subInd = 0; subInd < numSub; subInd++)
			{
				int numInd = ois.readInt();
				retval.subpops[subInd] = new Subpopulation();
				retval.subpops[subInd].individuals = new Individual[numInd];
				for(int indIndex = 0; indIndex < numInd; indIndex++)
				{
					Object ind = ois.readObject();
					if(!(ind instanceof Individual))
						throw new InvalidObjectException("The file contains an object that is not "
								+ "instance of Individual: " + ind.getClass().toString());
					retval.subpops[subInd].individuals[indIndex] = (Individual)ind;
				}
			}
		}
		
		return retval;
	}

	@Override
	public void close() throws Exception 
	{
		if(output != null)
		{
			output.flush();
			output.close();
			output = null;
			file = null; 
		}
	}
}
