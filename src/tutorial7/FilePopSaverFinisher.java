package tutorial7;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import ec.EvolutionState;
import ec.util.Parameter;

public class FilePopSaverFinisher extends PopSaverFinisher 
{
	private static final long serialVersionUID = 1L;
	/**
	 * The parameter that gives the name of the file that final population will be saved
	 * to. If this parameter is not given, <code>DEFAULT_FILE_NAME</code> will be used 
	 * instead. 
	 */
	public static final String P_FILE_NAME = "final-pop-file-name";
	
	/**
	 * The default file name that will be used if the <code>P_FILE_NAME</code>
	 * parameter is not specified. 
	 */
	public static final String DEFAULT_FILE_NAME = "last_population.dat";
	
	
	private PrintWriter writer = null; 
	
	@Override
	public void setup(EvolutionState state, Parameter base) 
	{
		Parameter fileNameParam = base.push(P_FILE_NAME);
		String fileName = state.parameters.getString(fileNameParam, null);
		fileName = fileName == null ? DEFAULT_FILE_NAME : fileName; 
		
		File file = new File(fileName);
		boolean result = true; 
		if(file.exists())
			result = file.delete();
		if(result == false) // failed to delete the file
		{
			state.output.fatal("Failed to delete the old file: " + fileName 
							   + " for " + getClass().getName());
		}
		try 
		{
			result = file.createNewFile();
		} catch (IOException e) 
		{
			state.output.fatal("Failed to create the destination file " 
								+  fileName + " for " + getClass().getName());
			e.printStackTrace();
		}
		if(result == false)
		{
			state.output.fatal("Failed to create new file " + fileName
								+ " for " + getClass().getName());
		}
		
		try {
			writer = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			// This should not happen as it is already checked before. 
			state.output.fatal(e.getMessage());
			e.printStackTrace();
		}
		
		super.setup(state, base);
	}

	@Override
	public PrintWriter getDestination() 
	{
		return writer;
	}

	@Override
	public boolean shouldCloseDestination() 
	{
		return true;
	}

}
