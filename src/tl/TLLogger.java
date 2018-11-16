package tl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
import ec.util.Parameter;
import tl.knowledge.KnowledgeItem;

public interface TLLogger<T>
{
	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	default int setupLogger(EvolutionState state, Parameter base)
	{

		try {
			Parameter knowledgeLogFileNameParam = base.push(P_KNOWLEDGE_LOG_FILE_NAME);
			String knowledgeLogFile = state.parameters.getStringWithDefault(
					knowledgeLogFileNameParam, null, "TLGPCriptorMutationLog");
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
			state.output.fatal("Failed to create knowledge log file in CodeFragmentBuilder: "
					+ e.getStackTrace().toString());
			return -1;
		}
	}

	default void log(EvolutionState state, KnowledgeItem<T> it, int cfCounter, int logID)
	{
		state.output.println(cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
		state.output.flush();
		state.output.println("", logID);
	}
}
