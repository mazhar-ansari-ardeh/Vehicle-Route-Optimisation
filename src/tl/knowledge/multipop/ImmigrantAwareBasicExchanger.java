package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.util.Parameter;

/**
 * This is an extension of the {@code ImmigrantAwareBasicExchanger} algorithm in which the algorithm also checks the
 * list of transferred individauls to see if the a candicate immigrant has already been transferred in the current
 * generation or not.
 */
public class ImmigrantAwareBasicExchanger extends BasicExchanger
{
    /**
     * A boolean parameter that, when set to {@code true}, the algorithm will allow sending individuals that are
     * duplicate in the target population. Doing so may have the benefit of improving the genotypic diversity of the
     * target population.
     */
    public static final String P_ALLOW_DUPLICATES = "allow-duplicates";
    private boolean allowDuplicates;

    @Override
    public void setup(EvolutionState state, Parameter _base)
    {
        super.setup(state, _base);

        allowDuplicates = state.parameters.getBoolean(_base.push(P_ALLOW_DUPLICATES), null, false);
        log(state, logID, true, String.format("Allow duplicate transfer: %b\n", allowDuplicates));
    }

    public Population postBreedingExchangePopulation(EvolutionState state)
    {
        log(state, logID, String.format("Gen: %d\n", state.generation));

        for( int subTo = 0 ; subTo < nImmigrants.length ; subTo++ )
        {

            if( nImmigrants[subTo] > 0 && chatty )
            {
                log(state, logID, "Immigrating " +  nImmigrants[subTo] + " individuals from mailbox for subpopulation " + subTo + "\n");
            }

            int len = state.population.subpops[subTo].individuals.length;
            // double check that we won't go into an infinite loop!
            if ( nImmigrants[subTo] >= state.population.subpops[subTo].individuals.length )
                state.output.fatal("Number of immigrants ("+nImmigrants[subTo] +
                        ") is larger than subpopulation #" + subTo + "'s size (" +
                        len +").  This would cause an infinite loop in the selection-to-die procedure.");

            int[] indices = selectToDie(state, subTo); // new int[ nImmigrants[subTo] ];

            HistoricMultiPopEvolutionState hstate = (HistoricMultiPopEvolutionState) state;
            for( int y = 0 ; y < nImmigrants[subTo] ; y++ )
            {
                Individual mutated = immigrants[subTo][y];
                log(state, logID, "Sending " + mutated.toString() + " to " + subTo + "\n");

                boolean isNew = false;
                for(int i=0; i < numMutations; i++)
                {
                    isNew = !hstate.isSeenInTransferred(subTo, mutated);
                    if(!isNew)
                        log(state, logID, "immiseen," + i + ",");

                    // If not seen in the immigrant list, check the history.
                    if(isNew)
                    {
                        isNew = !hstate.IsSeenInCurrentPop(subTo, mutated);
                        if (!isNew)
                            log(state, logID, "curpopseen," + i + ",");
                        else if (enableHistory)
                        {
                            isNew = hstate.isSeenIn(subTo, mutated) <= 0;
                            if(!isNew)
                                log(state, logID, "allpopseen," + i + ",");
                        }
                    }
                    if(isNew)
                    {
                        log(state, logID, "new," + i + "\n" );
                        log(state, logID, "replacing," + state.population.subpops[subTo].individuals[ indices[y] ].toString() + "\n\n");

                        mutated.evaluated = false;
                        hstate.receiveImmigrant(subTo, mutated, indices[y]);
                        break;
                    }
                    mutated = mutator.mutate(subTo, (GPIndividual) immigrants[subTo][y], state, 0);
                    log(state, logID, "mutated," + mutated.toString() + "\n");
                }
                if(!isNew && allowDuplicates)
                {
                    log(state, logID, "sending seen due to allowing duplicates\n" );
                    log(state, logID, "replacing," + state.population.subpops[subTo].individuals[ indices[y] ].toString() + "\n\n");

                    mutated.evaluated = false;
                    hstate.receiveImmigrant(subTo, mutated, indices[y]);
                }
                log(state, logID, "\n\n");
            }

            // reset the number of immigrants in the mailbox for the current subpopulation
            // this doesn't need another synchronization, because the thread is already synchronized
            nImmigrants[subTo] = 0;
        }

        return state.population;
    }

}
