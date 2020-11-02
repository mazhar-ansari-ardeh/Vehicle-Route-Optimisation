package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.SelectionMethod;
import ec.select.TournamentSelection;
import ec.util.Parameter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.MathArrays;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A selection method that returns a duplicate.
 *
 * At the moment, this class is intended to be used for the selection of individuals that will be replaced with the
 * immigrating ones.
 */
public class DuplicateSelection extends SelectionMethod
{
    // Maps subpopulation index to a tuple of individual and individual index in the subpopulation.
    List<Pair<Individual, Integer>> duplicates = new ArrayList<>();

    HashSet<Integer> selected = new HashSet<>();

    TournamentSelection tselect = new TournamentSelection();

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        tselect.setup(state, new Parameter("dup-select"));
        tselect.pickWorst = true;
    }

    @Override
    public void prepareToProduce(EvolutionState s, int subpopulation, int thread)
    {
        super.prepareToProduce(s, subpopulation, thread);
        if(!(s instanceof HistoricMultiPopEvolutionState))
            throw new RuntimeException("The evolution state does not support history search.");

        HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState)s;
//        ArrayList<Pair<Individual, Integer>> dups = duplicates.computeIfAbsent(subpopulation, integer -> new ArrayList<>());

        Individual[] individuals = s.population.subpops[subpopulation].individuals;
        for(int i = 0; i < individuals.length; i++)
        {
            // HistoricMultiPopEvolutionState.isSeen searches the current population too.
            if(hstate.isSeenIn(subpopulation, individuals[i]) <= 1)
                continue;

            boolean iHasDupInCurrentPop = false;
            int[] ci = hstate.characterise(individuals[i]);
            for (int j = i+1; j < individuals.length; j++)
            {
                int[] cj = hstate.characterise(individuals[j]);
                if(MathArrays.distance(ci, cj) == 0)
                {
                    duplicates.add(new ImmutablePair<>(individuals[j], j));
                    selected.add(j);
                    iHasDupInCurrentPop = true;
                }
            }

            // There is loose end here. The preceding operations check if i has a duplicate in the population and if
            // not, then the earlier generations are considered and searched below. But, what if i has a duplicate in
            // the current population and also, it itself is a duplicate too?
            if(!iHasDupInCurrentPop)
            {
                if(hstate.isSeenIn(subpopulation, individuals[i]) >= 1) {
                    duplicates.add(new ImmutablePair<>(individuals[i], i));
                    selected.add(i);
                }
            }
        }

        duplicates = duplicates.stream().distinct()
                               .sorted(Collections.reverseOrder(Comparator.comparingDouble(o -> o.getLeft().fitness.fitness()))).collect(Collectors.toList());
    }

    @Override
    public void finishProducing(EvolutionState s, int subpopulation, int thread) {
        super.finishProducing(s, subpopulation, thread);
        duplicates.clear();
        selected.clear();
    }

    @Override
    public int produce(int subpopulation, EvolutionState state, int thread)
    {
        if(duplicates == null || duplicates.isEmpty())
        {
            int retval= tselect.produce(subpopulation, state, thread);
            int counter = 1;
            while(selected.contains(retval) && counter <= 10)
            {
                retval= tselect.produce(subpopulation, state, thread);
                counter++;
            }
            selected.add(retval);
            return retval;
        }
        Pair<Individual, Integer> ind = duplicates.remove(0);
        return ind.getRight();
    }

    @Override
    public Parameter defaultBase()
    {
        return new Parameter("duplicate-select");
    }
}
