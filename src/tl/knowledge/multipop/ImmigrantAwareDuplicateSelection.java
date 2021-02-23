package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This is an extension of {@code DuplicateSelection} algorithm in which the {@code produce} method also checks the
 * list of transferred individuals when it wants to produce an individual and there is not duplicate left.
 */
public class ImmigrantAwareDuplicateSelection extends DuplicateSelection
{
    public static final String P_DEF_BASE = "imm-aware-duplicate-select";

    @Override
    public int produce(int subpopulation, EvolutionState state, int thread)
    {
        HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState) state;
        if(duplicates == null || duplicates.isEmpty())
        {
            int retval= tselect.produce(subpopulation, state, thread);
            int counter = 1;
            while(counter <= 10)
            {
                counter++;
                retval = tselect.produce(subpopulation, state, thread);
                if(!selected.contains(retval))
                {
                    selected.add(retval);
                    if (!hstate.isSeenInTransferred(
                            subpopulation, state.population.subpops[subpopulation].individuals[retval]))
                        break;
                    else
                        log(state, logID, String.format("Ignored transferred: %s\n",
                            state.population.subpops[subpopulation].individuals[retval].toString()));
                }
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
