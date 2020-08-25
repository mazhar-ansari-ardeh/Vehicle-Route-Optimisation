package tl.knowledge.sst;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;
import tl.TLLogger;
import tl.gp.GPIndividualUtils;
import tl.gp.TLGPIndividual;

import java.util.ArrayList;
import java.util.Comparator;

public class SSTBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	private int knowledgeSuccessLogID;

	/**
	 * The percentage of the initial population to be created from the individuals transferred from the source domain.
	 */
	public static String P_TRANSFER_PERCENT = "transfer-percent";
	private double transferPercent;

	/**
	 * The similarity threshold. If a new individual is within this distance of an already seen individual, then they
	 * are considered similar.
	 */
	public static String P_SIMILARITY_THRESHOLD = "similarity-thresh";
	private double similarityThreshold;

	private int populationSize;
	private static int cfCounter = 0;

	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);
		if (!(state instanceof SSTEvolutionState))
		{
			logFatal(state, knowledgeSuccessLogID, "The state is not of type SSTEvolutionState\n");
			return;
		}

		transferPercent = state.parameters.getDouble(base.push(P_TRANSFER_PERCENT), null);
		if(transferPercent < 0 || transferPercent > 1)
			logFatal(state, knowledgeSuccessLogID, "Invalid transfer percent: " + transferPercent + "\n");
		log(state, knowledgeSuccessLogID, true, "SSTBuilder: Transfer percent: " + transferPercent + "\n");

		similarityThreshold = state.parameters.getInt(base.push(P_SIMILARITY_THRESHOLD), null);
		if(similarityThreshold < 0)
			logFatal(state, knowledgeSuccessLogID,"Xover: Invalid similarity threshold: " + similarityThreshold);
		log(state, knowledgeSuccessLogID, true, "SSTBuilder: similarity threshold: " + similarityThreshold + "\n");

		populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
	}

	private ArrayList<GPRoutingPolicy> pop = null;

	@Override
	public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent, GPFunctionSet set,
								int argposition, int requestedSize)
	{
		SSTEvolutionState sstate = (SSTEvolutionState) state;
		if(sstate.createRandInd())
		{
			return super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
		}

		if(pop == null)
		{
			pop = new ArrayList<>(sstate.getTransferredIndividuals());
			pop.sort(Comparator.comparingDouble(i -> i.getGPTree().owner.fitness.fitness()));
		}

		if(cfCounter < populationSize * transferPercent && !pop.isEmpty())
		{
			GPIndividual ind = pop.remove(0).getGPTree().owner;
			ind = (GPIndividual) ind.clone();

			GPNode root = GPIndividualUtils.stripRoots(ind).get(0);

			cfCounter++;
			log(state, knowledgeSuccessLogID, cfCounter + ": \t" + ind.fitness.fitness() + ", " + root.makeLispTree()
					+ "\ntransferred from source\n\n");
			root.parent = parent;
			root.argposition = (byte) argposition;
			return root;
		}

		TLGPIndividual tlgpIndividual;
		GPNode root = null;
		do
		{
			if(root != null)
				log(state, knowledgeSuccessLogID, "Discarded seen random: " + root.makeLispTree() + "\n\n");
			root = super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);

			tlgpIndividual = GPIndividualUtils.asGPIndividual(root);
		}while(!sstate.isNew(tlgpIndividual, similarityThreshold));

		root.parent = parent;
		root.argposition = (byte) argposition;
		tlgpIndividual.trees[0].child = null;

		log(state, knowledgeSuccessLogID, "Unseen random: " + root.makeLispTree() + "\n\n");
		return root;
	}
}