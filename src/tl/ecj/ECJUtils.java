package tl.ecj;

import ec.Evaluator;
import ec.EvolutionState;
import ec.Evolve;
import ec.util.Parameter;
import ec.util.ParameterDatabase;

import java.util.ArrayList;

/**
 * Utility class to help working with ECJ.
 */
public class ECJUtils
{

	/**
	 * Loads an instance of ECJ with the parameter file given and also with the extra parameters given variable argument
	 * list
	 * @param paramFileNamePath The path (containing name) of the parameter file to load.
	 * @param ecjParams Extra parameters to pass to ECJ. The string items in this list cannot contain space characters
	 *                  and do not require the '-p' directive that is required to pass parameters to ECJ from command
	 *                  line.
	 * @return an object of {@code EvolutionState}.
	 */
	public static EvolutionState loadECJ(String paramFileNamePath, String... ecjParams)
	{
		ArrayList<String> params = new ArrayList<>();
		params.add("-file");
		params.add(paramFileNamePath);
		for(String param : ecjParams)
		{
			params.add("-p");
			params.add(param);
		}
		String[] processedParams = new String[params.size()];
		params.toArray(processedParams);
		ParameterDatabase parameters = Evolve.loadParameterDatabase(processedParams);

		EvolutionState state = Evolve.initialize(parameters, 0);

		Parameter p;

		// setup the evaluator, essentially the test evaluation model
		p = new Parameter(EvolutionState.P_EVALUATOR);
		state.evaluator = (Evaluator)(parameters.getInstanceForParameter(p, null, Evaluator.class));
		state.evaluator.setup(state, p);

		return state;
	}
}
