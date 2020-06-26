package tl.gp;

import ec.*;
import ec.util.*;
import tl.TLLogger;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import ec.gp.*;
import ec.gp.koza.MutationPipeline;

/**
 * This class, along with the {@code TLGPCriptorKB} and {@code TLGPCriptorBuilder} classes,
 * implement the idea in paper "<i>Reusing Extracted Knowledge in Genetic Programming to Solve
 * Complex Texture Image Classification Problems</i>".
 * @author mazhar
 *
 */
public class TLGPCriptorMutation extends MutationPipeline implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1;

	public static final String P_KNOWLEDGE_PROBABILITY = "knowledge-probability";

	private double knowledgeProbability;

	private int knowledgeSuccessLogID;

	public Object clone()
	{
		TLGPCriptorMutation c = (TLGPCriptorMutation)(super.clone());

		c.knowledgeProbability = knowledgeProbability;

		return c;
	}

	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state,base);

		Parameter p = base.push(P_KNOWLEDGE_PROBABILITY);
		knowledgeProbability = state.parameters.getDouble(p, null);

		knowledgeSuccessLogID = setupLogger(state, base, true);
	}

	public int produce(final int min,
			final int max,
			final int start,
			final int subpopulation,
			final Individual[] inds,
			final EvolutionState state,
			final int thread)
	{
		if(state.random[thread].nextDouble() < knowledgeProbability)
		{
			// grab individuals from our source and stick 'em right into inds.
			// we'll modify them from there
			int m = sources[0].produce(min,max,start,subpopulation,inds,state,thread);
			if(m != 1)
				state.output.fatal("This mutator accepts only one individual from its source.");

			// GPInitializer initializer = ((GPInitializer)state.initializer);
			GPIndividual i = (GPIndividual)inds[start];
			int numChildren = i.trees[0].child.children.length;
			if(numChildren <= 0)
				return super.produce(min, max, start, subpopulation, inds, state, thread);
			int selectedChild = state.random[thread].nextInt(numChildren);
			TLGPCriptorBuilder builder = new TLGPCriptorBuilder();
			KnowledgeExtractor extractor = builder.getKnowledgeExtractor();
			CodeFragmentKI item = (CodeFragmentKI) extractor.getNext();
			item.incrementCounter();
			GPNode node = item.getItem();
			GPIndividual j = i.lightClone();
			if(i.trees.length > 1)
				state.output.fatal("This mutator supports only individuals with one tree");

			log(state, knowledgeSuccessLogID, false, "Mutating: \n" + i.trees[0].child.makeLispTree() + "\n");
			j.trees = new GPTree[i.trees.length];
			j.trees[0] = i.trees[0].lightClone();
			j.trees[0].owner = j;
			j.trees[0].child = (GPNode) i.trees[0].child.clone();
			j.trees[0].child.parent = j.trees[0];
			j.trees[0].child.argposition = 0;
			j.trees[0].child.children[selectedChild] = node;
			node.parent = j.trees[0].child;
			node.argposition = (byte) selectedChild;
			j.evaluated = false;
			inds[start] = j;
//			cfCounter++;
			log(state, knowledgeSuccessLogID, false, "Mutated: \n" + node.makeLispTree() + "\n\n");
			return m;
		}
		else
			return super.produce(min, max, start, subpopulation, inds, state, thread);
	}

//	private static int cfCounter = 0;

//	private void log(EvolutionState state, CodeFragmentKI it, int logID)
//	{
//		state.output.println(cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
//		state.output.flush();
//		state.output.println("", logID);
//	}
}

