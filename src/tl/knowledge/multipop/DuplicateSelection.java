package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.SelectionMethod;
import ec.gp.GPNode;
import ec.select.TournamentSelection;
import ec.util.Parameter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tl.TLLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A selection method that returns a duplicate.
 *
 * At the moment, this class is intended to be used for the selection of individuals that will be replaced with the
 * immigrating ones. In this context, the prepareToProduce method of this class is invoked after the breeding operator
 * and as a result, it may return individuals that are not evaluated.
 */
public class DuplicateSelection extends SelectionMethod implements TLLogger<GPNode>
{
    public static final String P_DEF_BASE = "duplicate-select";
    /**
     * A boolean parameter that if set to {@code true}, the duplicate selection mechanism will consider the history of
     * all the previously seen individuals throughout the entire evolution. Otherwise, only the current population will
     * be considered to find the individuals that have duplicate in the current sub-population. The default value of
     * this parameter is {@code true}.
     */
    public static final String P_ENABLE_HISTORY_SEARCH = "enable-history-search";
    private boolean enableHistorySearch;

    /**
     * The base parameter for the selection mechanism. This class needs a selection mechanism to select the worst
     * individuals when there are no duplicates left. This selection mechanism will use this paratemer as its base to
     * load its configuration settings.
     */
    public static final String P_DUP_SELECT = "dup-select";

    // Maps subpopulation index to a tuple of individual and individual index in the subpopulation.
    List<Pair<Individual, Integer>> duplicates = new ArrayList<>();

    HashSet<Integer> selected = new HashSet<>();

    TournamentSelection tselect = new TournamentSelection();

    int logID;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);
        tselect.setup(state, base.push(P_DUP_SELECT));
        tselect.pickWorst = true;

        logID = setupLogger(state, base, true);

        enableHistorySearch = state.parameters.getBoolean(base.push(P_ENABLE_HISTORY_SEARCH), null, true);
        log(state, logID, true, String.format("Enable duplicate selection: %b\n", enableHistorySearch));
    }

    @Override
    public void prepareToProduce(EvolutionState s, int subpopulation, int thread)
    {
        HashSet<Integer> hashedInds = new HashSet<>();
        super.prepareToProduce(s, subpopulation, thread);
        if(!(s instanceof HistoricMultiPopEvolutionState))
            throw new RuntimeException("The evolution state does not support history search.");

        HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState)s;

        Individual[] individuals = s.population.subpops[subpopulation].individuals;
        for(int i = 0; i < individuals.length; i++)
        {
            // HistoricMultiPopEvolutionState updates the search history after evaluation but before breeding. As a
            // result, the history contains the information of the parent population but not the offspring population
            // (which is held in the current population. Therefore, it is needed to search the current population
            // manually.
            if(enableHistorySearch && hstate.isSeenIn(subpopulation, individuals[i]) >= 1)
            {
                duplicates.add(new ImmutablePair<>(individuals[i], i));
                selected.add(i);
                continue;
            }

            int[] ci = hstate.characterise(individuals[i]);
            int hash = Arrays.hashCode(ci);
            boolean wasNew = hashedInds.add(hash);
            if(!wasNew)
            {
//                System.out.println(Arrays.toString(ci));
                duplicates.add(new ImmutablePair<>(individuals[i], i));
                selected.add(i);
            }
        }

        duplicates = duplicates.stream().distinct()
                               .sorted(Collections.reverseOrder(
                                       Comparator.comparingDouble(o -> o.getLeft().fitness.fitness())))
                               .collect(Collectors.toList());
        log(hstate, logID, String.format("Gen %d, Found %d duplicates in %d subpopulation\n", s.generation,
                duplicates.size(), subpopulation));
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
                counter++;
            }
            selected.add(retval);
            log(state, logID, String.format("Producing unique: %s\n",
                    state.population.subpops[subpopulation].individuals[retval].toString()));
            return retval;
        }
        Pair<Individual, Integer> ind = duplicates.remove(0);
        log(state, logID, String.format("Producing duplicate: %s\n", ind.getLeft().toString()));
        return ind.getRight();
    }

    @Override
    public Parameter defaultBase()
    {
        return new Parameter(P_DEF_BASE);
    }
}
