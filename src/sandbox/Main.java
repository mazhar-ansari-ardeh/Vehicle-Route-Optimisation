package sandbox;

public class Main {


//
//	private static EvolutionState state = null;
//
//	static void loadECJ(String paramFileNamePath, String... ecjParams)
//	{
//		ArrayList<String> params = new ArrayList<>();
//		params.add("-file");
//		params.add(paramFileNamePath);
//		for(String param : ecjParams)
//		{
//			params.add("-p");
//			params.add(param);
//		}
//		String[] processedParams = new String[params.size()];
//		params.toArray(processedParams);
//		ParameterDatabase parameters = Evolve.loadParameterDatabase(processedParams);
//
//		state = Evolve.initialize(parameters, 0);
//
//		Parameter p;
//
//		// setup the evaluator, essentially the test evaluation model
//		p = new Parameter(EvolutionState.P_EVALUATOR);
//		state.evaluator = (Evaluator)
//				(parameters.getInstanceForParameter(p, null, Evaluator.class));
//		state.evaluator.setup(state, p);
//
//		p = new Parameter("eval.problem.eval-model.instances.0.samples");
//		int samples = state.parameters.getInt(p, null);
//		if(samples < 100)
//			state.output.fatal("Sample size is too small: " + samples);
//		else
//			state.output.warning("Sample size in AnalyzeTerminals: " + samples);
//	}
//

}
