package tl.knowledge.codefragment;

import java.util.ArrayList;
//import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import ec.EvolutionState;
import tl.knowledge.KnowledgeExtractor;

public class FrequentTournamentKnowledgeExtraction implements KnowledgeExtractor
{
	// private Iterator<Integer> iter;

	private Map<Integer, CodeFragmentKI> repository;

	int tournamentSize;

	EvolutionState state;
	int threadnum;

//	private KnowledgeItem<T>[] repositoryValues ;

	public FrequentTournamentKnowledgeExtraction(EvolutionState state, int threadnum
			, Map<Integer, CodeFragmentKI> repository, int tournamentSize)
	{
		 // iter = repository.keySet().iterator();
		 this.repository = repository;
		 // repositoryValues = (KnowledgeItem<T>[])repository.values().toArray();

		 this.threadnum = threadnum;
		 this.state = state;
		 this.tournamentSize = tournamentSize;
	}

	@Override
	public boolean hasNext()
	{
		return repository.size() != 0;
	}

	@Override
	public CodeFragmentKI getNext()
	{
		ArrayList<CodeFragmentKI> list = new ArrayList<>();
		CodeFragmentKI[] repositoryValues = (CodeFragmentKI[])repository.values().toArray();


		for(int i = 0; i < tournamentSize; i++)
		{
			state.random[threadnum].nextInt(repository.size());
			list.add(repositoryValues[i]);
		}
		return list.stream().sorted(
							(a, b)-> Long.compare(b.getDuplicateCount(), a.getDuplicateCount()))
				.collect(Collectors.toCollection(ArrayList::new)).get(0);
	}

	@Override
	public void reset()
	{
		 // iter = repository.keySet().iterator();
	}

}