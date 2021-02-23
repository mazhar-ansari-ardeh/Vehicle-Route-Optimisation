package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.select.TournamentSelection;
import ec.util.Parameter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tl.TLLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A modified version of tournament selection that only considers the unique individuals in the given population and
 * ignores the duplicates.
 */
public class UniqueTournamentSelection extends TournamentSelection implements TLLogger<GPNode>
{
    public static final String P_DEFAULT_BASE = "unique-tselect";

    // Maps subpopulation index to a tuple of individual and individual index in the subpopulation.
    List<Pair<Individual, Integer>> uniques = new ArrayList<>();

    private int lastGenPrepared = -1;

    @Override
    public void prepareToProduce(EvolutionState s, int subpopulation, int thread)
    {
        if(lastGenPrepared == s.generation)
            return;

        lastGenPrepared = s.generation;
        uniques.clear();

        super.prepareToProduce(s, subpopulation, thread);

        if(!(s instanceof HistoricMultiPopEvolutionState))
            throw new RuntimeException("The evolution state does not support history search.");
        HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState)s;

        HashSet<Integer> hashedInds = new HashSet<>();
        Individual[] individuals = s.population.subpops[subpopulation].individuals;

        for(int i = 0; i < individuals.length; i++)
        {
            int[] ci = hstate.characterise(individuals[i]);
            int hash = Arrays.hashCode(ci);
            boolean wasNew = hashedInds.add(hash);
            if(wasNew)
            {
                uniques.add(new ImmutablePair<>(individuals[i], i));
            }
        }
    }

//    @Override
//    public void finishProducing(EvolutionState s, int subpopulation, int thread)
//    {
//        super.finishProducing(s, subpopulation, thread);
////        uniques.clear();
//    }

    private int getRandomIndividual(EvolutionState state, int thread)
    {
        return state.random[thread].nextInt(uniques.size());
    }

    @Override
    public int produce(int subpopulation, EvolutionState state, int thread)
    {
        int best = getRandomIndividual(state, thread);

        int s = getTournamentSizeToUse(state.random[thread]);

        if (pickWorst)
            for (int x=1;x<s;x++)
            {
                int j = getRandomIndividual(state, thread);
                if (!betterThan(uniques.get(j).getKey(), uniques.get(best).getKey(), subpopulation, state, thread))  // j is at least as bad as best
                    best = j;
            }
        else
            for (int x=1;x<s;x++)
            {
                int j = getRandomIndividual(state, thread);
                if (betterThan(uniques.get(j).getKey(), uniques.get(best).getKey(), subpopulation, state, thread))  // j is better than best
                    best = j;
            }

        return best;
    }

    @Override
    public Parameter defaultBase() {
        return new Parameter(P_DEFAULT_BASE);
    }
}
