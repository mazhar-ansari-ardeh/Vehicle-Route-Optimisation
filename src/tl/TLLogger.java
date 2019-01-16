package tl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
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
	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	default int setupLogger(EvolutionState state, Parameter base)
	{

		try {
			Parameter knowledgeLogFileNameParam = base.push(P_KNOWLEDGE_LOG_FILE_NAME);
			String knowledgeLogFile = state.parameters.getString(knowledgeLogFileNameParam, null);
			File successKnLog = new File(knowledgeLogFile + ".succ.log");
			if(successKnLog.exists())
				successKnLog.delete();

			Path pathToSuccFile = successKnLog.toPath();
			Path pathToSuccDir = pathToSuccFile.getParent();
			if(pathToSuccDir != null)
			{
				File statDirFile = pathToSuccDir.toFile();
				if(statDirFile.exists() == false && statDirFile.mkdirs() == false)
					state.output.fatal("Failed to create stat directory: "
							+ pathToSuccDir.toString());
			}

			successKnLog.createNewFile();

			return state.output.addLog(successKnLog, false);
		}
		catch (IOException e) {
			e.printStackTrace();
			state.output.fatal("Failed to create knowledge log file");
			return -1;
		}
	}

	default void log(EvolutionState state, KnowledgeItem<T> it, int cfCounter, int logID)
	{
		state.output.println("CFCounter: " + cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}

	default void log(EvolutionState state, T it, int logID, String... messages)
	{
		state.output.print("[item: " + (it == null ? "null" : it.toString()), logID);
		for(String message : messages)
			state.output.print(message + ", ", logID);
		state.output.flush();
		state.output.println("]", logID);
	}
}
