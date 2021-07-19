package tl.knowledge.sst;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.gp.koza.MutationPipeline;
import ec.util.Parameter;
import tl.TLLogger;
import tl.gphhucarp.dms.DMSSaver;

public class SSTMutation extends MutationPipeline implements TLLogger<GPNode>
{
	/**
	 * The probability of accepting a newly mutated individual that is already been seen before.
	 * The mutation operator plays an important role in the GP evolution. One main goal of this operator is to help GP
	 * search in the vicinity of the already-found solutions with the hope that a better solution could be found there.
	 * The basic idea of forcing GP to move into unseen regions can be drastic because if the mutation operator does not
	 * find an unseen solution, then a randomly created individual will be selected to replace the
	 * individual-to-be-mutated. This could lead to hazardous randomness. As a result, this parameter allows GP to
	 * accept a previously-seen mutated individual to be accepted.
	 */
	public static final String P_PROB_ACCEPT_SEEN = "prob-accept-seen";
	private double probAcceptSeen;

	/**
	 * Similarity threshold. All items within this similarity threshold will be considered as seen. This threshold is
	 * used for determining if a mutated individual is seen before or not.
	 */
	public static final String P_SIMILARITY_THRESHOLD = "similarity-thresh";
	double similarityThreshold;

	/**
	 * Number of times that the mutation operator will try to find an individual that is not seen before. If the operator
	 * does not find the a new individual after that, it will create a random individual and return it instead.
	 */
	public static final String P_NUM_TRIES = "sst-num-tries";
	private int numSSTTries;

	private int knowledgeSuccessLogID;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);

		similarityThreshold = state.parameters.getDouble(base.push(P_SIMILARITY_THRESHOLD), null);
		log(state, knowledgeSuccessLogID, true, "Niche radius " + similarityThreshold + "\n");

		numSSTTries = state.parameters.getInt(base.push(P_NUM_TRIES), null);
		if(numSSTTries <= 0)
			logFatal(state,knowledgeSuccessLogID, "Invalid number of tries: " + numSSTTries + "\n");
		log(state, knowledgeSuccessLogID, true, "Number of tries: " + numSSTTries + "\n");

		probAcceptSeen = state.parameters.getDouble(base.push(P_PROB_ACCEPT_SEEN), null);
		if(probAcceptSeen < 0 || probAcceptSeen > 1)
			logFatal(state, knowledgeSuccessLogID,"Invalid probability: " + probAcceptSeen + "\n");
		log(state, knowledgeSuccessLogID, true, "Probability of accepting the seen: " + probAcceptSeen + "\n");

//		int dmsSize = state.parameters.getInt(base.push(P_DMS_SIZE), null);
//		log(state, knowledgeSuccessLogID, true, "DMS size: " + dmsSize + "\n");

		if(!(state instanceof DMSSaver))
		{
			String message = "Evolution state must be of type DMSaver for this builder\n";
			log(state, knowledgeSuccessLogID, message);
			state.output.fatal(message);
		}
	}

	public int produce(final int min, final int max, final int start, final int subpopulation, final Individual[] inds,
					   final EvolutionState state, final int thread)
	{
		// grab individuals from our source and stick 'em right into inds.
		// we'll modify them from there
		int n = sources[0].produce(min,max,start,subpopulation,inds,state,thread);
		SSTEvolutionState sstate = (SSTEvolutionState) state;

		// should we bother?
		if (!state.random[thread].nextBoolean(likelihood))
			return reproduce(n, start, subpopulation, inds, state, thread, false);  // DON'T produce children from source -- we already did

		GPInitializer initializer = ((GPInitializer)state.initializer);

		// now let's mutate 'em
		for(int q=start; q < n+start; q++)
		{
			SSTIndividual i = (SSTIndividual)inds[q];
//			log(state, knowledgeSuccessLogID, false, "Mutating:\n" + i.trees[0].child.makeLispTree() + "\n");

			int t;
			// pick random tree
			if (tree==TREE_UNFIXED)
				if (i.trees.length>1) t = state.random[thread].nextInt(i.trees.length);
				else t = 0;
			else t = tree;

			SSTIndividual j = null;

			log(state, knowledgeSuccessLogID, "Mutating:\n" + i.trees[0].child.makeLispTree() + "\n");

			SSTIndividual mutated = null;
			for (int tries = 0; tries < numSSTTries; tries++)
			{
				mutated = mutate(i, subpopulation, state, thread, initializer, t);
				if(sstate.isNew(mutated, similarityThreshold))
				{
					j = mutated;
					j.setOrigin(IndividualOrigin.MutationUnseen);
					log(state, knowledgeSuccessLogID, false, "Unseen:\n" + mutated.trees[0].child.makeLispTree() + "\n");
					break;
				}
				else
					log(state, knowledgeSuccessLogID, false, "Seen:\n"
							+ mutated.trees[0].child.makeLispTree() + "\n");
			}
			assert mutated != null;
			if(j == null)
			{
				if(state.random[thread].nextDouble() < probAcceptSeen)
				{
					j = mutated;
					j.setOrigin(IndividualOrigin.MutationSeen);
					log(state, knowledgeSuccessLogID, false, "Accepting a seen:\n" + j.trees[0].child.makeLispTree() + "\n");
				} else
				{
					j = newRandInd(state, thread, i, t);
					j.setOrigin(IndividualOrigin.MutationRandom);
					log(state, knowledgeSuccessLogID, false, "Accepting a random:\n" + j.trees[0].child.makeLispTree() + "\n");
				}
			}

			// add the new individual, replacing its previous source
			inds[q] = j;
		}
		return n;
	}

	private SSTIndividual newRandInd(EvolutionState state, int thread, SSTIndividual i, int t)
	{
		int size = GPNodeBuilder.NOSIZEGIVEN;
		SSTEvolutionState sstate = (SSTEvolutionState) state;
		if (equalSize) size = i.trees[t].child.numNodes(GPNode.NODESEARCH_ALL);
		GPInitializer initializer = ((GPInitializer)state.initializer);
		for (int tries = 0; tries < numSSTTries; tries++)
		{
			i.trees[t].child.parent = null;
			i.trees[t].child = builder.newRootedTree(state,
					i.trees[t].constraints(initializer).treetype,
					thread,
					i.trees[t],
					i.trees[t].constraints(initializer).functionset,
					0,
					size);

			if(sstate.isNew(i, similarityThreshold))
				return i;
		}

		i.trees[t].child.parent = null;
		i.trees[t].child = builder.newRootedTree(state,
				i.trees[t].constraints(initializer).treetype,
				thread,
				i.trees[t],
				i.trees[t].constraints(initializer).functionset,
				0,
				size);

		return i;
	}

	SSTIndividual mutate(SSTIndividual i, int subpopulation,
					  EvolutionState state, int thread, GPInitializer initializer, int treeIndex)
	{
		if (tree!=TREE_UNFIXED && (tree<0 || tree >= i.trees.length))
			// uh oh
			state.output.fatal("GP Mutation Pipeline attempted to fix tree.0 to a value which was out of " +
					"bounds of the array of the individual's trees.  Check the pipeline's fixed tree values -- " +
					"they may be negative or greater than the number of trees in an individual");

		// validity result...
		boolean res = false;

		// prepare the nodeselector
		nodeselect.reset();

		// pick a node

		GPNode p1=null;  // the node we pick
		GPNode p2=null;

		for(int x = 0; x< numSSTTries; x++)
		{
			// pick a node in individual 1
			p1 = nodeselect.pickNode(state,subpopulation,thread,i,i.trees[treeIndex]);

			// generate a tree swap-compatible with p1's position

			int size = GPNodeBuilder.NOSIZEGIVEN;
			if (equalSize) size = p1.numNodes(GPNode.NODESEARCH_ALL);

			p2 = builder.newRootedTree(state,
					p1.parentType(initializer),
					thread,
					p1.parent,
					i.trees[treeIndex].constraints(initializer).functionset,
					p1.argposition,
					size);

			// check for depth and swap-compatibility limits
			res = verifyPoints(p2,p1);  // p2 can fit in p1's spot  -- the order is important!

			// did we get something that had both nodes verified?
			if (res) break;
		}

		SSTIndividual j;

		if (sources[0] instanceof BreedingPipeline)
		// it's already a copy, so just smash the tree in
		{
			j=i;
			if (res)  // we're in business
			{
				p2.parent = p1.parent;
				p2.argposition = p1.argposition;
				if (p2.parent instanceof GPNode)
					((GPNode)(p2.parent)).children[p2.argposition] = p2;
				else ((GPTree)(p2.parent)).child = p2;
				j.evaluated = false;  // we've modified it
			}
		}
		else // need to clone the individual
		{
			j = (i.lightClone());

			// Fill in various tree information that didn't get filled in there
			j.trees = new GPTree[i.trees.length];

			// at this point, p1 or p2, or both, may be null.
			// If not, swap one in.  Else just copy the parent.
			for(int x=0;x<j.trees.length;x++)
			{
				if (x==treeIndex && res)  // we've got a tree with a kicking cross position!
				{
					j.trees[x] = (i.trees[x].lightClone());
					j.trees[x].owner = j;
					j.trees[x].child = i.trees[x].child.cloneReplacingNoSubclone(p2,p1);
					j.trees[x].child.parent = j.trees[x];
					j.trees[x].child.argposition = 0;
					j.evaluated = false;
				} // it's changed
				else
				{
					j.trees[x] = (i.trees[x].lightClone());
					j.trees[x].owner = j;
					j.trees[x].child = (GPNode)(i.trees[x].child.clone());
					j.trees[x].child.parent = j.trees[x];
					j.trees[x].child.argposition = 0;
				}
			}
		}

		return j;
	}
}
