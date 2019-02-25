package tl.gp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import javafx.util.Pair;
import tl.TLLogger;
import tl.knowledge.*;
import tl.knowledge.codefragment.CodeFragmentKI;

/**
 *
 * @author mazhar
 *
 */
public class ContribSubtreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

	/**
	 * The percentage of initial population that is created from extracted knowledge. The value must
	 * be in range (0, 1].
	 */
	public static final String P_TRANSFER_PERCENT = "transfer-percent";


	/**
	 * The path to the file that contains subtree information.
	 */
	public static final String P_KNOWLEDGE_FILE = "knowledge-file";


	private HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> subtrees;

	private static KnowledgeExtractor extractor = null;


	private int knowledgeSuccessLogID;

	/**
	 * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
	 * population that is transfered from extracted knowledge. The value must be in range (0, 1].
	 */
	private double transferPercent;

	private static int cfCounter = 0;

	@SuppressWarnings("unchecked")
	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base);

		Parameter p = base.push(P_TRANSFER_PERCENT);
		transferPercent = state.parameters.getDouble(p, null);

		String knowFile = state.parameters.getString(p.push(P_KNOWLEDGE_FILE), null);
		if(knowFile == null)
			state.output.fatal("Knowledge file name cannot be null");
		loadSubtrees(knowFile);


//		FrequentCodeFragmentKB knowledgeBase =
//				new FrequentCodeFragmentKB(state, extractPercent, minDepth, maxDepth);
//		knowledgeBase.extractFrom(kbDirectory, ".bin", extractionMethod);
//		extractor = knowledgeBase.getKnowledgeExtractor();
//		state.output.warning("DepthedFrequentSimpleCodeFragmentBuilder loaded. Transfer percent: "
//							 + transferPercent + ", extraction method: " + extractionMethod
//							 + ", min depth: " + minDepth + ", max depth: " + maxDepth);
	}


	/**
	 * This structure contains the knowledge transfered from source domain. The key is the
	 * transferred subtree and the value is the weight of the subtree based on its contribution in
	 * the source domain.
	 */
	HashMap<TLGPIndividual, Double> knowledgeBase = new HashMap<>();

	@SuppressWarnings("unchecked")
	private void loadSubtrees(String knowFile)
	{
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(knowFile)))
		{
			double minFit = ois.readDouble(); // read minFit
			double maxFit = ois.readDouble(); // read maxFit
			subtrees = (HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>>) ois.readObject();

			calculateWeights(subtrees, minFit, maxFit);

		} catch (IOException|ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	HashMap<TLGPIndividual, Double> weights = new HashMap<>();

	void calculateWeights(HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> subtree,
			double minFit, double maxFit)
	{
		double gmax = 1.0 / (1 + minFit);
		double gmin = 1.0 / (1 + maxFit);

		for(GPIndividual ind : subtree.keySet())
		{
			ArrayList<Pair<GPNode, Double>> subs = subtree.get(ind);
			for(Pair<GPNode, Double> pair : subs)
			{
				TLGPIndividual i = GPIndividualUtils.asGPIndividual(pair.getKey());
				double contrib = pair.getValue();
				double nfit = 1f / (ind.fitness.fitness() + 1);
				nfit = Math.max(0, (nfit - gmin)/(gmax - gmin));
				// contrib is (oldfit - newfit) so a positive value means positive impact on fitness
				if(contrib > 0.001)
				{
					if(weights.containsKey(i))
						weights.put(i, weights.get(i) + nfit);
					else
						weights.put(i, nfit);
				}
			}
		}
	}


	@Override
	public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
			final GPNodeParent parent, final GPFunctionSet set,	final int argposition,
			final int requestedSize)
	{
		int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		int numToTransfer = (int) Math.round(popSize * transferPercent);
		if(numToTransfer >= 0 && cfCounter < numToTransfer)
		{
			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
			if(cf != null)
			{
				cfCounter++;
				log(state, cf, cfCounter, knowledgeSuccessLogID);
				GPNode node = cf.getItem();
				node.parent = parent;
				return node;
			}
			else
				log(state, null, cfCounter, knowledgeSuccessLogID);
		}
		if (state.random[thread].nextDouble() < pickGrowProbability)
			return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
							type,thread,parent,argposition,set);
		else
			return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,
							type,thread,parent,argposition,set);
	}

}