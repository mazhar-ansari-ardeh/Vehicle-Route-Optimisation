package tl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
import ec.util.Log;
import ec.util.Parameter;
import tl.knowledge.KnowledgeItem;

/**
 * TLLogger (Transfer Learning Logger) is a utility trait that adds the logging functionality to
 * a knowledge-related class. This trait adds the 'knowledge-log-file' parameter to the base of
 * the class that implements this. <p>
 * Any class that implements this interface needs to call the {@code TLLogger.setupLogger} method
 * to perform the setup tasks required by this interface.<p>
 * This class only manages one logger.
 *
 * @author mazhar
 *
 * @param <T> is the underlying type of the knowledge item.
 */
public interface TLLogger<T>
{
	String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	default int setupLogger(EvolutionState state, Parameter base, boolean zip)
	{
		return setupLogger(state, base, P_KNOWLEDGE_LOG_FILE_NAME, zip);
	}

	default int setupLogger(EvolutionState state, Parameter base)
	{
		return setupLogger(state, base, P_KNOWLEDGE_LOG_FILE_NAME, false);
	}

	default int setupLogger(EvolutionState state, String fileName)
	{
		if(fileName == null || fileName.isEmpty())
			throw new RuntimeException("Log file name cannot be empty.");
		try
		{
			File file = new File(fileName);
			if (file.exists() && !file.delete())
			{
				throw new RuntimeException("The log file" + fileName + " already exists and failed to delete it");
			}

			Path pathToSuccFile = file.toPath();
			Path pathToSuccDir = pathToSuccFile.getParent();
			if (pathToSuccDir != null)
			{
				File statDirFile = pathToSuccDir.toFile();
				if (!statDirFile.exists() && !statDirFile.mkdirs())
					state.output.fatal("Failed to create stat directory: "
							+ pathToSuccDir.toString());
			}

			state.output.warning("Log file created: " + file.getAbsolutePath());

			return state.output.addLog(file, false, true);
		}
		catch (IOException e) {
			e.printStackTrace();
			state.output.fatal("Failed to create knowledge log file");
			return -1;
		}
	}

	default int setupLogger(EvolutionState state, Parameter base, String fileParamName, boolean zip)
	{
		if(fileParamName == null || fileParamName.isEmpty())
			throw new RuntimeException("Log file name cannot be empty.");

		try {
			Parameter knowledgeLogFileNameParam = base.push(fileParamName);
			String knowledgeLogFile = state.parameters.getString(knowledgeLogFileNameParam, null);
			if(knowledgeLogFile == null)
				throw new RuntimeException("log file not specified.");
			else
				state.output.warning("Log file name: " + knowledgeLogFile);

			File successKnLog = new File(knowledgeLogFile + ".succ.log");
			if(successKnLog.exists() && !successKnLog.delete())
				throw new RuntimeException("The log file" + knowledgeLogFile + " already exists and failed to delete it");

			Path pathToSuccFile = successKnLog.toPath();
			Path pathToSuccDir = pathToSuccFile.getParent();
			if(pathToSuccDir != null)
			{
				File statDirFile = pathToSuccDir.toFile();
				if(!statDirFile.exists() && !statDirFile.mkdirs())
					state.output.fatal("Failed to create stat directory: "
							+ pathToSuccDir.toString());
			}

			if(!successKnLog.createNewFile())
				throw new RuntimeException("Failed to create the log file: " + successKnLog.getAbsolutePath());
			state.output.warning("Log file created: " + successKnLog.getAbsolutePath());

			return state.output.addLog(successKnLog, false, zip);
		}
		catch (IOException e) {
			e.printStackTrace();
			state.output.fatal("Failed to create knowledge log file");
			return -1;
		}
	}

	default void closeLogger(EvolutionState state, int id)
	{
		state.output.flush();
		Log logger = state.output.getLog(id);
		if (!logger.isLoggingToSystemOut)
		{
			logger.writer.close();
		}
	}

	default void log(EvolutionState state, int logID, String... messages)
	{
		log(state, logID, false, messages);
	}

	default void log(EvolutionState state, int logID, boolean logSysout, String... messages)
	{
		for(int i = 0; i < messages.length; i++)
		{
			state.output.print(messages[i] + (i == messages.length - 1 ? "" : ", "), logID);
			if(logSysout)
				state.output.warning(messages[i] + (i == messages.length - 1 ? "" : ", "));
		}
		state.output.flush();
	}

	@Deprecated
	default void log(EvolutionState state, KnowledgeItem<T> it, int cfCounter, int logID)
	{
		state.output.println("CFCounter: " + cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}
	@Deprecated
	default void log(EvolutionState state, T it, int logID, String... messages)
	{
		state.output.print("[item: " + (it == null ? "null" : it.toString()), logID);
		for(String message : messages)
			state.output.print(message + ", ", logID);
		state.output.flush();
		state.output.println("]", logID);
	}
}
