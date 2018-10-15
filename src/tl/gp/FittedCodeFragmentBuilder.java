package tl.gp;

import java.io.File;

import ec.EvolutionState;
import ec.Fitness;
import ec.gp.GPProblem;
import ec.util.Parameter;
import tl.knowledge.*;
import tl.knowledge.codefragment.CodeFragmentKB;
import tl.knowledge.codefragment.fitted.*;

public class FittedCodeFragmentBuilder extends CodeFragmentBuilder
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";
	public static final String P_KNOWLEDGE_TOURNAMENT_SIZE = "knowledge-tournament-size";

	/**
	 * The default value for tournament size of the knowledge base. This value will be
	 * used if the <code>P_KNOWLEDGE_TOURNAMENT_SIZE</code> is not present.
	 */
	public static final int DEFAULT_KNOWLEDGE_TOURNAMENT_SIZE = 10;

	private static KnowledgeExtractor extractor = null;


	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter knowledgeFileName = base.push(P_KNOWLEDGE_FILE);
		String fileName = state.parameters.getString(knowledgeFileName, null);
		if(fileName == null)
		{
			state.output.fatal("Failed to load parameter", knowledgeFileName);
		}
		File kbFile = new File(fileName);
		if(kbFile.exists() == false)
		{
			state.output.fatal("Knowledge file does not exist: "
								+ fileName, knowledgeFileName);
		}

		int tournamentSize = state.parameters.getIntWithDefault(
				base.push(P_KNOWLEDGE_TOURNAMENT_SIZE), null, DEFAULT_KNOWLEDGE_TOURNAMENT_SIZE);



		Parameter problemParam = new Parameter("pop.subpop.0.species.fitness");
		Fitness fitness = (Fitness) state.parameters.getInstanceForParameter(problemParam,
				 null, Fitness.class);
		fitness.setup(state, problemParam);
	    CodeFragmentKB knowledgeBase = new FittedCodeFragmentKB(state
	    		, (GPProblem)state.evaluator.p_problem, fitness, tournamentSize);

		Parameter knowledgeExtraction = base.push("knowledge-extraction");
		String extraction = state.parameters.getString(knowledgeExtraction, null);
		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);
		if(extractionMethod != KnowledgeExtractionMethod.AllSubtrees &&
		   extractionMethod != KnowledgeExtractionMethod.RootSubtree)
		{
			state.output.fatal("Invalid value for parameter knowledge-extraction: " + extraction
							   + "Acceptable values are: 'all' and 'root'.");
			return;
		}

		knowledgeBase.addFrom(kbFile, state, extractionMethod);
		extractor  = knowledgeBase.getKnowledgeExtractor();

		state.output.warning("FittedCodeFragmentBuilder loaded. Tournament size: " + tournamentSize);
	}


	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return extractor;
	}

}
