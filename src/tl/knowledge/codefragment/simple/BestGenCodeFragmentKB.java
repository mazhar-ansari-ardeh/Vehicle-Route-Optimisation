package tl.knowledge.codefragment.simple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ec.Individual;
import ec.Population;
import ec.gp.*;
import tl.gp.KnowledgeExtractionMethod;
import tl.gp.PopulationWriter;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKB;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.SinglePassKnowledgeExtractor;

public class BestGenCodeFragmentKB extends CodeFragmentKB
{
	ConcurrentHashMap<Integer, CodeFragmentKI> repository = new ConcurrentHashMap<>();

	private final int k;

	public BestGenCodeFragmentKB(int k)
	{
		if(k < 1)
			throw new IllegalArgumentException("K must be a positive value");

		this.k = k;
	}

	@Override
	protected boolean addItem(GPNode item)
	{
		throw new RuntimeException("Not supported");
	}

	@Override
	public boolean extractFrom(GPIndividual gpIndividual, KnowledgeExtractionMethod method)
	{
		throw new RuntimeException("Not supported");
	}

	public boolean extractFrom(Path knowledgeDirectory, String fileExtention)
	{
		if(knowledgeDirectory == null )
			throw new NullPointerException("Knowledge directory cannot be null");
		if(fileExtention == null || fileExtention.trim().isEmpty())
			throw new IllegalArgumentException("File extension cannot be null or empty");

		if(knowledgeDirectory.toFile().isFile())
			throw new IllegalArgumentException("Received a file object for knowledge directory");

		boolean added = false;
		try
		{
			ArrayList<Path> regularFilePaths = Files.list(knowledgeDirectory)
				.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().endsWith(fileExtention) || file.getFileName().toString().endsWith(".cf"))
				.collect(Collectors.toCollection(ArrayList::new));
			for(Path path : regularFilePaths)
			{
				Population p = PopulationWriter.loadPopulation(path.toString());
				Comparator<Individual> com2 = (Individual o1, Individual o2) ->
				{
					if(o1.fitness.fitness() < o2.fitness.fitness())
						return -1;
					if(o1.fitness.fitness() == o2.fitness.fitness())
						return 0;

					return 1;
				};

				Arrays.sort(p.subpops[0].individuals, com2);
				for(int i = 0; i < k && i <p.subpops[0].individuals.length; i++)
				{
					add((GPIndividual) p.subpops[0].individuals[i], path.getFileName().toString());
					added = true;
				}
			}

		}
		catch(IOException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		return added;
	}

	private void add(GPIndividual individual, String origin)
	{
		GPNode root =  individual.trees[0].child;
		root.parent = null;
		individual.trees[0].child = null;

		CodeFragmentKI cf = new CodeFragmentKI(root, origin);
		repository.put(cf.hashCode(), cf);
	}

	@Override
	public boolean removeItem(GPNode item)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(GPNode item)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean extractFrom(Population population, KnowledgeExtractionMethod method)
	{
		throw new RuntimeException("Not supported");
	}

	@Override
	public KnowledgeExtractor getKnowledgeExtractor()
	{
		SinglePassKnowledgeExtractor<GPNode> extractor = new SinglePassKnowledgeExtractor<>(repository);
		return extractor;
	}
}