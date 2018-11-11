package tl.gp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ec.EvolutionState;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.knowledge.codefragment.simple.MySimpleCodeFragmentKB;

public class TLGPCriptorKB extends HalfBuilder
{
	private static final long serialVersionUID = 1L;
	
	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

	private int knowledgeSuccessLogID;
	
	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		
		Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getIntWithDefault(transferPercentParam, null, -1);

		MySimpleCodeFragmentKB knowledgeBase = new MySimpleCodeFragmentKB(state, transferPercent);

		knowledgeBase.extractFrom(kbFile, extractionMethod);
		extractor = knowledgeBase.getKnowledgeExtractor();
		state.output.warning("MYSimpleCodeFragmentBuilder loaded. Transfer percent: "
							 + transferPercent + ", extraction method: " + extractionMethod
							 + ", transfer percent: " + transferPercent);

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

			Path pathToFailFile = successKnLog.toPath();
			Path pathToFailDir = pathToFailFile.getParent();
			if(pathToFailDir != null)
			{
				File statDirFile = pathToFailDir.toFile();
				if(statDirFile.exists() == false && statDirFile.mkdirs() == false)
					state.output.fatal("Failed to create stat directory: "
									   + pathToFailDir.toString());
			}

			successKnLog.createNewFile();

			knowledgeSuccessLogID = state.output.addLog(successKnLog, false);
		}
		 catch (IOException e) {
				state.output.fatal("Failed to create knowledge log file in CodeFragmentBuilder: "
						+ e.getStackTrace().toString());
			}

}
