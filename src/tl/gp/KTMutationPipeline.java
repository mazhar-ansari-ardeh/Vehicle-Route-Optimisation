package tl.gp;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.gp.koza.MutationPipeline;
import ec.util.Parameter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tl.TLLogger;
import tl.gp.hash.VectorialAlgebraicHashCalculator;
import tl.gp.simplification.AlgebraicTreeSimplifier;
import tl.knowledge.KnowledgeExtractionMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class KTMutationPipeline extends MutationPipeline implements TLLogger<GPNode>
{
	/**
	 * When the knowledge source is a directory, this parameter specifies from which generation on
	 * the populations should be read. This parameter is inclusive. If the knowledge path is to a file, this parameter
	 * is ignored and can be omitted.
	 */
	public final String P_GENERATION_FROM = "from-generation";
	/**
	 * When the knowledge source is a directory, this parameter specifies the generation until which
	 * the populations should be read. This parameter is inclusive. If the knowledge path is to a file, this parameter
	 * is ignored and can be omitted.
	 */
	public final String P_GENERATION_TO = "to-generation";

	/**
	 * The percent of the top individuals of the source domain to consider for extraction of subtrees. For example,
	 * given a 1024 individuals from a source domain, if the value of this parameter is 0.2, then 202 individuals will
	 * be selected from the 1024 individuals with a tournament selection. The value of this parameter is in the range of
	 * [0, 1].
	 */
	public final String P_EXTEACTION_PERCENT = "extract-percent";

	/**
	 * The size of the tournament that is used for selecting the transferred subtrees. The selection is based on the
	 * frequency with which the subtrees appeared in the source domain. This parameter cannot be zero or negative.
	 */
	public final String P_TOURNAMENT_SIZE = "tournament-size";
	private int tournamentSize;

	/**
	 * A boolean parameter that if {@code true}, the loaded trees from the source domain will be simplified before
	 * subtree extraction.
	 */
	public final String P_SIMPLIFY = "simplify";
	private boolean simplify = true;

//	/**
//	 * Niche radius. All items within this radius of a niche center will be cleared. The niche radius is used only for
//	 * clearing the newly-created intermediate pool.
//	 */
//	public final String P_NICHE_RADIUS = "interim-niche-radius";
//	double nicheRadius;
//
//	/**
//	 * The capacity of each niche that is used for clearing. This parameter is only used during the clearing process of
//	 * the intermediate population.
//	 */
//	public final String P_NICHE_CAPACITY = "interim-niche-capacity";
//	int nicheCapacity;

	/**
	 * The path to the file or directory that contains GP populations.
	 */
	public static final String P_KNOWLEDGE_PATH = "knowledge-path";

	/**
	 * Extraction method. The acceptable methods are {@code KnowledgeExtractionMethod.AllSubtrees},
	 * {@code KnowledgeExtractionMethod.RootSubtree} and {@code KnowledgeExtractionMethod.Root}.
	 */
	public static final String P_KNOWLEDGE_EXTRACTION = "knowledge-extraction";

	private int knowledgeSuccessLogID;

	/**
	 * The repository of extracted subtrees. The key is the hashcode of the subtree and the value is a pair of the
	 * subtree itself and its frequency.
	 */
	HashMap<Integer, Pair<GPNode, Integer>> repository = new HashMap<>();

	public void setup(final EvolutionState state, final Parameter base)
	{
		super.setup(state, base);

		knowledgeSuccessLogID = setupLogger(state, base, true);

//		nicheRadius = state.parameters.getDouble(base.push(P_NICHE_RADIUS), null);
//		log(state, knowledgeSuccessLogID, true, "Interim niche radius " + nicheRadius + "\n");

//		nicheCapacity = state.parameters.getInt(base.push(P_NICHE_CAPACITY), null);
//		log(state, knowledgeSuccessLogID, true, "Interim niche capacity " + nicheCapacity + "\n");

		int fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
		int toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
		log(state, knowledgeSuccessLogID, true, "Load surrogate pool from generation " + fromGeneration +
				", to generation " + toGeneration + "\n");

		String extraction = state.parameters.getString(base.push(P_KNOWLEDGE_EXTRACTION), null);
		KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);
		log(state, knowledgeSuccessLogID, true, extractionMethod.toString() + "\n");

		double extractPercent = state.parameters.getDouble(base.push(P_EXTEACTION_PERCENT), null);
		if(extractPercent <= 0 || extractPercent > 1)
		{
			log(state, knowledgeSuccessLogID, true, "Invalid extraction percent: " + extractPercent);
			throw new RuntimeException("Invalid extraction percent: " + extractPercent);
		}
		log(state, knowledgeSuccessLogID, true, "Extraction percent: " + extractPercent);

		String kbFile = state.parameters.getString(base.push(P_KNOWLEDGE_PATH), null);
		if (kbFile == null)
		{
			state.output.fatal("Knowledge path cannot be null");
			return;
		}

		List<Individual> inds;
		try
		{
			// I like a large and diverse KNN pool so I set the niche radius to zero so that policies that are very
			// similar and are potentially duplicate are discarded without being too strict about it.
			inds = PopulationUtils.loadPopulations(state, kbFile, fromGeneration, toGeneration, this,
					knowledgeSuccessLogID, true);
			if(inds == null || inds.isEmpty())
				throw new RuntimeException("Could not load the saved populations");

		} catch (IOException | ClassNotFoundException e)
		{
			e.printStackTrace();
			state.output.fatal("Failed to load the population from: " + kbFile);
			return;
		}

		simplify = state.parameters.getBoolean(base.push(P_SIMPLIFY), null, true);
		log(state, knowledgeSuccessLogID, true, "Simplify: " + simplify);

		tournamentSize = state.parameters.getInt(base.push(P_TOURNAMENT_SIZE), null);
		if(tournamentSize <= 0)
		{
			log(state, knowledgeSuccessLogID, true, "Tournament size must be a positive value: " + tournamentSize + "\n");
			throw new RuntimeException("Tournament size must be a positive value: " + tournamentSize + "\n");
		}
		log(state, knowledgeSuccessLogID, true, "Tournament size: " + tournamentSize + "\n");

		int populationSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
		log(state, knowledgeSuccessLogID, true, "Population size: " + populationSize + "\n");
		int sampleSize = (int) Math.floor(populationSize * extractPercent);
		extractSubtrees(inds, extractionMethod, sampleSize, state);
	}

	private void extractSubtrees(List<Individual> inds, KnowledgeExtractionMethod extractionMethod,
								 int sampleSize, EvolutionState state)
	{
		inds.sort(Comparator.comparingDouble(i -> i.fitness.fitness()));
		List<GPIndividual> ginds = inds.stream().map(i -> (GPIndividual) i).collect(Collectors.toList());
		ginds = PopulationUtils.rankSelect(ginds, sampleSize);

		VectorialAlgebraicHashCalculator hc = new VectorialAlgebraicHashCalculator(state, 0, 100, 100000);
		AlgebraicTreeSimplifier ts = new AlgebraicTreeSimplifier(hc);

		for(GPIndividual i : ginds)
		{
			if(simplify)
			{
				log(state, knowledgeSuccessLogID, false, "Loaded tree before simplification: \n");
				log(state, knowledgeSuccessLogID, false, i.trees[0].child.makeLispTree() + "\n");
				ts.simplifyTree(state, i);
				log(state, knowledgeSuccessLogID, false, "Loaded tree after simplification: \n");
				log(state, knowledgeSuccessLogID, false, i.trees[0].child.makeLispTree() + "\n\n");
			}
			ArrayList<GPNode> allNodes = new ArrayList<>();
			switch (extractionMethod)
			{
				case ExactCodeFragment:
					throw new RuntimeException("The ExactCodeFragment extraction method is not supported.");
				case Root:
					allNodes.add(i.trees[0].child);
					break;
				case RootSubtree:
					allNodes.addAll(TreeSlicer.sliceRootChildrenToNodes(i, false));
					break;
				case AllSubtrees:
					allNodes.addAll(TreeSlicer.sliceAllToNodes(i, false));
					break;
			}

			for(GPNode node : allNodes)
			{
				int hashCode = node.rootedTreeHashCode();
				Pair<GPNode, Integer> pair = repository.get(hashCode);
				if (pair == null)
				{
					pair = new MutablePair<>(node, 1);
				} else
				{
					pair.setValue(pair.getValue() + 1);
				}
				repository.put(hashCode, pair);
			}
		}
	}

	/**
	 * Selects a subtree from a list of subtrees with a tournament selection based on the frequency of the subtrees.
	 * @param subtrees a list of subtree-frequency pairs to select from.
	 * @param state the evolution state of this process
	 * @param thread the running thread.
	 * @param size the tournament size.
	 * @return A selected subtree.
	 */
	public static int tournamentSelect(List<Pair<GPNode, Integer>> subtrees, final EvolutionState state, final int thread, int size)
	{
		if(subtrees == null || subtrees.size() == 0)
			throw new IllegalArgumentException("The individual set cannot be null or empty.");

		if(size <= 0)
			throw new IllegalArgumentException("Tournament size needs to be a positive number: " + size);

		int best = state.random[thread].nextInt(subtrees.size());

		for (int x=1; x < size; x++)
		{
			int j = state.random[thread].nextInt(subtrees.size());
			if (subtrees.get(j).getValue() > (subtrees.get(best).getValue()))  // j is better than best
				best = j;
		}

		return best;
	}

	private GPNode getSubtree(final EvolutionState state,
							  final int thread,
							  final int maxSize)
	{
		List<Pair<GPNode, Integer>> subtrees =
				repository.values().stream().filter(pair -> pair.getKey().depth() <= maxSize)
										.sorted(Comparator.comparingInt(Pair::getValue)).collect(Collectors.toList());

		if(subtrees.isEmpty())
		{
			return null; // So that the caller can do as it wishes.
		}

		int i = tournamentSelect(subtrees, state, thread, tournamentSize);
		log(state, knowledgeSuccessLogID, false, "Subtree selected for mutation:\n");
		log(state, knowledgeSuccessLogID, false
				 , subtrees.get(i).getKey().makeLispTree() + "\nFreq: " + subtrees.get(i).getValue() + "\n");

		return (GPNode) subtrees.get(i).getKey().clone();
	}

	public int produce(final int min,
					   final int max,
					   final int start,
					   final int subpopulation,
					   final Individual[] inds,
					   final EvolutionState state,
					   final int thread)
	{
		double rnd = state.random[thread].nextDouble();
		if(rnd < 0.5)
			return super.produce(min,max, start, subpopulation,inds,state, thread);

		// grab individuals from our source and stick 'em right into inds.
		// we'll modify them from there
		int n = sources[0].produce(min,max,start,subpopulation,inds,state,thread);

		// should we bother?
		if (!state.random[thread].nextBoolean(likelihood))
			return reproduce(n, start, subpopulation, inds, state, thread, false);  // DON'T produce children from source -- we already did


		GPInitializer initializer = ((GPInitializer)state.initializer);

		// now let's mutate 'em
		for(int q=start; q < n+start; q++)
		{
			GPIndividual i = (GPIndividual)inds[q];

			if (tree!=TREE_UNFIXED && (tree<0 || tree >= i.trees.length))
				// uh oh
				state.output.fatal("GP Mutation Pipeline attempted to fix tree.0 to a value which was out of bounds of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may be negative or greater than the number of trees in an individual");


			int t;
			// pick random tree
			if (tree==TREE_UNFIXED)
				if (i.trees.length>1) t = state.random[thread].nextInt(i.trees.length);
				else t = 0;
			else t = tree;

			// validity result...
			boolean res = false;

			// prepare the nodeselector
			nodeselect.reset();

			// pick a node

			GPNode p1=null;  // the node we pick
			GPNode p2=null;

			log(state, knowledgeSuccessLogID, false, "Mutating:\n" + i.trees[t].child.makeLispTree() + "\n");

			for(int x=0;x<numTries;x++)
			{
				// pick a node in individual 1
				p1 = nodeselect.pickNode(state,subpopulation,thread,i,i.trees[t]);

				// generate a tree swap-compatible with p1's position


				int size = GPNodeBuilder.NOSIZEGIVEN;
				if (equalSize) size = p1.numNodes(GPNode.NODESEARCH_ALL);

				// The depth at which the mutation point is located.
				int mutationDepth = i.trees[t].child.depth() - p1.depth();
				int treeDepthLimit = maxDepth - mutationDepth;

				p2 = getSubtree(state, thread, treeDepthLimit);
				if(p2 == null)
				{
					p2 = builder.newRootedTree(state,
							p1.parentType(initializer),
							thread,
							p1.parent,
							i.trees[t].constraints(initializer).functionset,
							p1.argposition,
							size);
				}

				// check for depth and swap-compatibility limits
				res = verifyPoints(p2,p1);  // p2 can fit in p1's spot  -- the order is important!

				// did we get something that had both nodes verified?
				if (res) break;
			}

			GPIndividual j;

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
					if (x==t && res)  // we've got a tree with a kicking cross position!
					{
						j.trees[x] = i.trees[x].lightClone();
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

			log(state, knowledgeSuccessLogID, false, "Mutated:\n" + j.trees[t].child.makeLispTree() + "\n\n");
			// add the new individual, replacing its previous source
			inds[q] = j;
		}
		return n;
	}
}
