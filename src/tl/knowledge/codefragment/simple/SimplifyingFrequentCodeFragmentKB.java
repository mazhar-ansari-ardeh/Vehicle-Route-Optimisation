package tl.knowledge.codefragment.simple;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import tl.gp.PopulationUtils;
import tl.gp.simplification.AlgebraicTreeSimplifier;
import tl.gp.TreeSlicer;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.*;

/**
 * This class, with {@code SimplifyingFrequentCodeFragmentBuilder}, implements the idea of using
 * code fragments based on the frequency of their occurrence in source domain.
 * <p>
 * In this class, code fragments are extracted based on either Root, RootSubtree or All
 * extraction methods. <p>
 * The class also can limit code fragment extraction to with min and max depth limit. <p>
 * This class uses individual fitness values for sorting the source individuals but does not make
 * any assumptions about the type of fitness value (whether it is from train or test scenario).
 * @author mazhar
 */
@Deprecated // I cannot remember why I started working on this class but it is not important now. This is heavily modified.
public class SimplifyingFrequentCodeFragmentKB extends CodeFragmentKB
{
	// Using ConcurrentHashMap instead of HashMap will make this KB capable of
	// concurrency.
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	EvolutionState state;

	/**
	 * Minimum limit on depth of a subtree to extract. Subtrees that have length
	 * less than this are ignored.
	 */
	private int minDepth = -1;

	/**
	 * Maximum limit on depth of a subtree to extract. Subtrees that have length
	 * greater than this are ignored.
	 */
	private int maxDepth = -1;

	/**
	 * Percentage of top performing individuals to select for extraction.
	 */
	private double topPercentage;


	private double topGenerationPercent = 1;


	private boolean simplify = false;

	private AlgebraicTreeSimplifier simplifier = null;

	private int tournamentSize = 20;

	/**
	 * Creates a new {@code DepthedFrequentSimpleCodeFragmentKB} object
	 * @param state The evolutionary state
	 * @param topPercentage percentage of top performing individuals to select for extraction.
	 * @param minDepth minimum limit on depth of a subtree to extract. Subtrees that have length
	 * less than this are ignored.
	 * @param maxDepth maximum limit on depth of a subtree to extract. Subtrees that have length
	 * greater than this are ignored.
	 */
	public SimplifyingFrequentCodeFragmentKB(EvolutionState state, double topPercentage,
			int minDepth, int maxDepth)
	{
		this.state = state;
		if(minDepth > maxDepth)
			throw new IllegalArgumentException("Min depth limit cannot be higher than max depth "
					+ "limit.");

		if(topPercentage <= 0 || topPercentage > 1)
			throw new IllegalArgumentException("Top percentage must be a value in (0, 1]: "
												+ topPercentage);

		this.topPercentage = topPercentage;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}


	/**
	 * Creates a new {@code DepthedFrequentSimpleCodeFragmentKB} object
	 * @param state The evolutionary state
	 * @param topPercentage percentage of top performing individuals to select for extraction.
	 * @param minDepth minimum limit on depth of a subtree to extract. Subtrees that have length
	 * less than this are ignored.
	 * @param maxDepth maximum limit on depth of a subtree to extract. Subtrees that have length
	 * greater than this are ignored.
	 * @param threadnum The thread number for ECJ thread that is running this class objects. This
	 * parameter is used for generating random numbers and if {@code simplify} is {@code false}, it
	 * can be ignored.
	 * @param simplify if true, the algorithm will simplify and remove redundant subtrees from the
	 * an individual before adding it to the repository.
	 */
	public SimplifyingFrequentCodeFragmentKB(EvolutionState state, double topPercentage
			, double topGenerationPercent, int minDepth
			, int maxDepth, int threadnum, boolean simplify, int tournamentSize)
	{
		this.state = state;
		if(minDepth > maxDepth)
			throw new IllegalArgumentException("Min depth limit cannot be higher than max depth "
					+ "limit.");

		if(topPercentage <= 0 || topPercentage > 1)
			throw new IllegalArgumentException("Top percentage must be a value in (0, 1]: "
												+ topPercentage);
		this.topPercentage = topPercentage;
		this.topGenerationPercent = topGenerationPercent;
		this.tournamentSize = tournamentSize;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
		this.simplify = simplify;
//		if(simplify)
//		{
//			simplifier = new AlgebraicTreeSimplifier(state, threadnum);
	}

	@Override
	public boolean extractFrom(File file, KnowledgeExtractionMethod method)
	{
		throw new UnsupportedOperationException("This operation is not supported. Only, extraction "
				+ "from a directory is supported.");
	}

	public boolean extractFrom(Path knowledgeDirectory, KnowledgeExtractionMethod method)
	{
		if(knowledgeDirectory == null )
			throw new NullPointerException("Knowledge directory cannot be null");
//		if(fileExtention == null || fileExtention.trim().isEmpty())
//			throw new IllegalArgumentException("File extension cannot be null or empty");

		if(knowledgeDirectory.toFile().isFile())
			throw new IllegalArgumentException("Received a file object for knowledge directory");

		boolean added = false;
		try
		{
			ArrayList<Path> regularFilePaths = Files.list(knowledgeDirectory)
				.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().endsWith(".bin"))
				.sorted((a, b) ->{
					String fileName1 = a.toString();
					int index = fileName1.indexOf("gen.") + 4;
					int num1 = Integer.parseInt(fileName1.substring(index, fileName1.indexOf(".bin")));

					String fileName2 = b.toString();
					index = fileName2.indexOf("gen.") + 4;
					int num2 = Integer.parseInt(fileName2.substring(index, fileName2.indexOf(".bin")));

					return Long.compare(num1, num2);})
				.collect(Collectors.toCollection(ArrayList::new));

			if(regularFilePaths.isEmpty())
				state.output.fatal("Knowledge directory is empty: " + knowledgeDirectory.toString());

			regularFilePaths = regularFilePaths
					.stream().skip(
							(long)(regularFilePaths.size() * (1 - topGenerationPercent)))
							.collect(Collectors.toCollection(ArrayList::new));
			for(Path path : regularFilePaths)
			{
				Population p = PopulationUtils.loadPopulation(path.toString());
				Comparator<Individual> com2 = (Individual o1, Individual o2) ->
				{
					if(o1.fitness.fitness() < o2.fitness.fitness())
						return -1;
					if(o1.fitness.fitness() == o2.fitness.fitness())
						return 0;

					return 1;
				};

				Arrays.sort(p.subpops[0].individuals, com2);
				added |= extractFrom(p, method);
			}

		}
		catch(IOException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		return added;
	}

	@Override
	public boolean extractFrom(Population p, KnowledgeExtractionMethod method)
	{
		System.out.println("Inside FrequentCodeFragmentKB.addFrom");
		if (p == null)
		{
			System.out.println("Population is null. Exiting");
			System.err.println("Population is null. Exiting");
			return false;
		}

		// This class does support fitness-based sorting but it does make any assumptions about
		// the domain of fitness, whether it is on test scenario or train scenario.
		Comparator<Individual> comp = (Individual o1, Individual o2) ->
		{
			if(o1.fitness.fitness() < o2.fitness.fitness())
				return -1;
			if(o1.fitness.fitness() == o2.fitness.fitness())
				return 0;

			return 1;
		};

		Arrays.sort(p.subpops[0].individuals, comp);

		boolean added = false;
		int sampleSize = (int) Math.round(topPercentage * p.subpops[0].individuals.length);
//		state.output.warning("Sample size in FrequentCodeFragmentKB.addFrom: " + sampleSize);
//		System.out.println("Sample size in FrequentCodeFragmentKB.addFrom: " + sampleSize);
		for(int i = 0; i < sampleSize; i++)
		{
			added |= extractFrom((GPIndividual)p.subpops[0].individuals[i], method);
		}

		return added;
	}


	/**
	 * Extracts code fragments from an individual.
	 * @param gpIndividual the individual to extract codes from
	 * @param method extraction method. The acceptable methods are
	 * {@code KnowledgeExtractionMethod.AllSubtrees}, {@code KnowledgeExtractionMethod.RootSubtree}
	 * and {@code KnowledgeExtractionMethod.Root}.
	 */
	public boolean extractFrom(GPIndividual gpIndividual, KnowledgeExtractionMethod method)
	{
		if (gpIndividual == null)
		{
			return false;
		}

		if(simplify)
		{
//			simplifier.simplifyTree(gpIndividual.trees[0].child); As it is deprecated now, it is not important if the simplification is implemented or not.
		}

		ArrayList<GPNode> allNodes = null;
		switch(method)
		{
		case Root:
			return addItem(gpIndividual.trees[0].child);
		case RootSubtree:
			allNodes = TreeSlicer.sliceRootChildrenToNodes(gpIndividual, false);
			break;
		case AllSubtrees:
			allNodes = TreeSlicer.sliceAllToNodes(gpIndividual, false);
			break;
		default:
			throw new IllegalArgumentException("Given extraction method is not supported: "
												+ method);
		}

		boolean added = false;
		for(GPNode node : allNodes)
		{
			int depth = node.depth();
			if((minDepth <= 0 || depth >= minDepth) &&  (maxDepth <= 0 || depth <= maxDepth))
				added |= addItem(node);
		}
		return added;
	}


	/**
	 * Adds a new item to the repository. If the repository contains the given item,
	 * the method will not modify the knowledge base and returns <code>false</code>.
	 *
	 * @param item The item to be stored into this knowledge base. If this parameter
	 * is <code>null</code> the method will ignore them and return <code>false</code>.
	 *
	 * @return If the item is added successfully the return value will be <code>true
	 * </code> and otherwise, it will be <code>false</code>.
	 *
	 * @author Mazhar
	 */
	public boolean addItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		CodeFragmentKI cfItem = new CodeFragmentKI(item);
		if(repository.containsKey(cfItem.hashCode()))
		{
			repository.get(cfItem.hashCode()).incrementDuplicateCount();
			return false;
		}
		repository.put(cfItem.hashCode(), cfItem);


		return true;
	}


	/**
	 * Removes a given <code>KnowledgeItem</code> from knowledge base.
	 *
	 * @param item An item to be removed. If the parameter is <code>null</code> or
	 * is not an instance of <code>CodeFragmentKB</code>, the method ignores it
	 * and returns <code>false</code>.
	 *
	 * @return <code>true</code> if the item is successfully removed from this
	 * knowledge base and <code>false</code> otherwise.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean removeItem(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		return repository.remove(item.hashCode()) != null;
	}

	/**
	 * Checks if this knowledge base contains a given code fragment or not.
	 *
	 * @param item An item to be checked. If the parameter is <code>null</code>, the method ignores
	 * it and returns <code>false</code>.
	 *
	 * @return <code>true</code> if this knowledge base contains the item and <code>false</code>
	 * otherwise.
	 *
	 * @author Mazhar
	 */
	@Override
	public boolean contains(GPNode item)
	{
		if (item == null)
		{
			return false;
		}

		return repository.containsKey(item.hashCode());
	}

	/**
	 * Checks to see if this knowledge base is empty or not.
	 *
	 * @return <code>true</code> if the knowledge base is empty and <code>false</code> otherwise.
	 * @author Mazhar
	 */
	@Override
	public boolean isEmpty()
	{
		return repository.size() == 0;
	}

	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		return new FrequentTournamentKnowledgeExtraction(state, 0, repository, tournamentSize);
	}

	public class CodeFragmentKnowledgeExtractor implements KnowledgeExtractor
	{
		Iterator<Integer> iter;

		private Iterator<Integer> getIterator()
		{
			LinkedHashMap<Integer, CodeFragmentKI> rep = repository.entrySet().stream().sorted((entry1, entry2) ->
			{
				int entry1Count = entry1.getValue().getDuplicateCount();
				int entry2Count = entry2.getValue().getDuplicateCount();
				if(entry1Count < entry2Count)
					return 1;
				if(entry1Count > entry2Count)
					return -1;
				return 0;
			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			return rep.keySet().iterator();
		}

		public CodeFragmentKnowledgeExtractor()
		{
			iter = getIterator();
		}

		@Override
		public boolean hasNext()
		{
			return !repository.isEmpty();
		}

		@Override
		public CodeFragmentKI getNext()
		{
			if(iter.hasNext())
			{
				CodeFragmentKI retval = repository.get(iter.next());
				retval.incrementCounter();
				return retval;
			}
			else
				return null;
		}

		@Override
		public void reset()
		{
			 iter = getIterator();
		}
	}
}
