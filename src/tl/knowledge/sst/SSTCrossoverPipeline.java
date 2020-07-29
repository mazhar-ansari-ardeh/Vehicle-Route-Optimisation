package tl.knowledge.sst;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.gp.koza.CrossoverPipeline;
import ec.util.Parameter;
import tl.TLLogger;

public class SSTCrossoverPipeline extends CrossoverPipeline implements TLLogger<GPNode>
{
	public static String P_SIMILARITY_THRESHOLD = "similarity-thresh";
	private double similarityThreshold;

	/**
	 * Number of times that the crossover operator will try to find an individual that is not seen before. If the operator
	 * does not find a new individual after that, it will create a random individual and return it instead.
	 */
	public static final String P_NUM_TRIES = "sst-num-tries";
	private int numSSTTries;

	private int knowledgeSuccessLogID;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);
		if (!(state instanceof SSTEvolutionState))
		{
			logFatal(state, knowledgeSuccessLogID, "The state is not of type SSTEvolutionState\n");
			return;
		}

		similarityThreshold = state.parameters.getInt(base.push(P_SIMILARITY_THRESHOLD), null);
		if (similarityThreshold < 0)
			logFatal(state, knowledgeSuccessLogID, "SSTXover: Invalid similarity threshold: " + similarityThreshold);
		log(state, knowledgeSuccessLogID, true, "SSTXover: similarity threshold: " + similarityThreshold + "\n");

		numSSTTries = state.parameters.getInt(base.push(P_NUM_TRIES), null);
		if (numSSTTries <= 0)
			logFatal(state, knowledgeSuccessLogID, "Invalid number of tries: " + numSSTTries + "\n");
		log(state, knowledgeSuccessLogID, true, "Number of tries: " + numSSTTries + "\n");
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread)
	{
		// how many individuals should we make?
		int n = typicalIndsProduced();
		if (n < min) n = min;
		if (n > max) n = max;

		// should we bother?
		if (!state.random[thread].nextBoolean(likelihood))
			// DO produce children from source -- we've not done so already
			return reproduce(n, start, subpopulation, inds, state, thread, true);


		GPInitializer initializer = ((GPInitializer) state.initializer);

		for (int q = start; q < n + start; /* no increment */)  // keep on going until we're filled up
		{
			// grab two individuals from our sources
			if (sources[0] == sources[1])  // grab from the same source
				sources[0].produce(2, 2, 0, subpopulation, parents, state, thread);
			else // grab from different sources
			{
				sources[0].produce(1, 1, 0, subpopulation, parents, state, thread);
				sources[1].produce(1, 1, 1, subpopulation, parents, state, thread);
			}

			// at this point, parents[] contains our two selected individuals

			// are our tree values valid?
			if (tree1 != TREE_UNFIXED && (tree1 < 0 || tree1 >= parents[0].trees.length))
				// uh oh
				state.output.fatal("GP Crossover Pipeline attempted to fix tree.0 to a value which was out of bounds " +
						"of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may " +
						"be negative or greater than the number of trees in an individual");
			if (tree2 != TREE_UNFIXED && (tree2 < 0 || tree2 >= parents[1].trees.length))
				// uh oh
				state.output.fatal("GP Crossover Pipeline attempted to fix tree.1 to a value which was out of bounds " +
						"of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may " +
						"be negative or greater than the number of trees in an individual");

			int t1 = 0;
			int t2 = 0;
			if (tree1 == TREE_UNFIXED || tree2 == TREE_UNFIXED)
			{
				do
				// pick random trees  -- their GPTreeConstraints must be the same
				{
					if (tree1 == TREE_UNFIXED)
						if (parents[0].trees.length > 1)
							t1 = state.random[thread].nextInt(parents[0].trees.length);
						else t1 = 0;
					else t1 = tree1;

					if (tree2 == TREE_UNFIXED)
						if (parents[1].trees.length > 1)
							t2 = state.random[thread].nextInt(parents[1].trees.length);
						else t2 = 0;
					else t2 = tree2;
				} while (parents[0].trees[t1].constraints(initializer) != parents[1].trees[t2].constraints(initializer));
			} else
			{
				t1 = tree1;
				t2 = tree2;
				// make sure the constraints are okay
				if (parents[0].trees[t1].constraints(initializer)
						!= parents[1].trees[t2].constraints(initializer)) // uh oh
					state.output.fatal("GP Crossover Pipeline's two tree choices are both specified by the user " +
							"-- but their GPTreeConstraints are not the same");
			}

			GPIndividual[] retvals = new GPIndividual[2];
			doXover(state, subpopulation, thread, initializer, n, q, start, t1, t2, retvals);
			inds[q] = retvals[0];
			q++;
			if (q < n + start && !tossSecondParent)
			{
				inds[q] = retvals[1];
				q++;
			}
		}
		return n;
	}

	private void doXover(EvolutionState state, int subpopulation, int thread, GPInitializer initializer,
						 int n, int q, int start, int t1, int t2, GPIndividual[] retvals)
	{
		assert retvals != null && retvals.length >= 2;
		SSTEvolutionState sstate = (SSTEvolutionState) state;

		GPIndividual j1 = null;
		GPIndividual j2 = null;

		boolean foundUnseen = false;

		for (int tries = 0; tries < numSSTTries; tries++)
		{
			boolean res1 = false;
			boolean res2 = false;

			// prepare the nodeselectors
			nodeselect1.reset();
			nodeselect2.reset();

			GPNode p1 = null;
			GPNode p2 = null;

			for (int x = 0; x < numTries; x++)
			{
				// pick a node in individual 1
				p1 = nodeselect1.pickNode(state, subpopulation, thread, parents[0], parents[0].trees[t1]);

				// pick a node in individual 2
				p2 = nodeselect2.pickNode(state, subpopulation, thread, parents[1], parents[1].trees[t2]);

				// check for depth and swap-compatibility limits
				res1 = verifyPoints(initializer, p2, p1);  // p2 can fill p1's spot -- order is important!
				if (n - (q - start) < 2 || tossSecondParent) res2 = true;
				else res2 = verifyPoints(initializer, p1, p2);  // p1 can fill p2's spot -- order is important!

				if (res1 && res2)
					break;
			}

			j1 = parents[0].lightClone();
			j2 = null;
			if (n - (q - start) >= 2 && !tossSecondParent) j2 = parents[1].lightClone();

			// Fill in various tree information that didn't get filled in there
			j1.trees = new GPTree[parents[0].trees.length];
			if (n - (q - start) >= 2 && !tossSecondParent)
			{
				assert j2 != null;
				j2.trees = new GPTree[parents[1].trees.length];
			}

			for (int x = 0; x < j1.trees.length; x++)
			{
				if (x == t1 && res1)  // we've got a tree with a kicking cross position!
				{
					j1.trees[x] = parents[0].trees[x].lightClone();
					j1.trees[x].owner = j1;
					j1.trees[x].child = parents[0].trees[x].child.cloneReplacing(p2, p1);
					j1.trees[x].child.parent = j1.trees[x];
					j1.trees[x].child.argposition = 0;
					j1.evaluated = false;
				}  // it's changed
				else
				{
					j1.trees[x] = parents[0].trees[x].lightClone();
					j1.trees[x].owner = j1;
					j1.trees[x].child = (GPNode) (parents[0].trees[x].child.clone());
					j1.trees[x].child.parent = j1.trees[x];
					j1.trees[x].child.argposition = 0;
				}
			}

			if (n - (q - start) >= 2 && !tossSecondParent)
			{
				assert j2 != null;
				for (int x = 0; x < j2.trees.length; x++)
				{
					if (x == t2 && res2)  // we've got a tree with a kicking cross position!
					{
						j2.trees[x] = parents[1].trees[x].lightClone();
						j2.trees[x].owner = j2;
						j2.trees[x].child = parents[1].trees[x].child.cloneReplacing(p1, p2);
						j2.trees[x].child.parent = j2.trees[x];
						j2.trees[x].child.argposition = 0;
						j2.evaluated = false;
					} // it's changed
					else
					{
						j2.trees[x] = parents[1].trees[x].lightClone();
						j2.trees[x].owner = j2;
						j2.trees[x].child = (GPNode) (parents[1].trees[x].child.clone());
						j2.trees[x].child.parent = j2.trees[x];
						j2.trees[x].child.argposition = 0;
					}
				}
			}
			if (sstate.isNew(j1, similarityThreshold) && j2 != null && sstate.isNew(j2, similarityThreshold))
			{
				foundUnseen = true;
				if(j1 instanceof SSTIndividual)
					((SSTIndividual)j1).setOrigin(IndividualOrigin.CrossOverUnseen);
				if(j2 instanceof SSTIndividual)
					((SSTIndividual)j2).setOrigin(IndividualOrigin.CrossOverUnseen);
				break;
			}
			else
			{
				log(state, knowledgeSuccessLogID, "Seen: " + j1.trees[0].child.makeLispTree() + "\n");
				if(j2 != null)
					log(state, knowledgeSuccessLogID, "Seen: " + j2.trees[0].child.makeLispTree() + "\n");
			}
		} // for(int tries ...)

		if(!foundUnseen)
		{
			if(j1 instanceof SSTIndividual)
				((SSTIndividual)j1).setOrigin(IndividualOrigin.CrossOverSeen);
			if(j2 instanceof SSTIndividual)
				((SSTIndividual)j2).setOrigin(IndividualOrigin.CrossOverSeen);
		}
		retvals[0] = j1;
		retvals[1] = j2;
	}
}
