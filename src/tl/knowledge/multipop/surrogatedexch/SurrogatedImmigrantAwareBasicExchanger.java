package tl.knowledge.multipop.surrogatedexch;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.util.Parameter;
import tl.gp.TLGPIndividual;
import tl.knowledge.multipop.BasicExchanger;
import tl.knowledge.multipop.HistoricMultiPopEvolutionState;
import tl.knowledge.surrogate.SuGPIndividual;

import java.util.*;

public class SurrogatedImmigrantAwareBasicExchanger extends BasicExchanger
{
    @Override
    public void setup(EvolutionState state, Parameter _base) {
        super.setup(state, _base);

        candidateImmigrants = new ArrayList<>();
        for (int i = 0; i < immigrants.length; i++) {
            candidateImmigrants.add(new HashMap<>());
        }
    }

    ArrayList<Map<Integer, Individual>> candidateImmigrants;

    public Population preBreedingExchangePopulation(EvolutionState state)
    {
        // exchange individuals between subpopulations
        // BUT ONLY if the modulo and offset are appropriate for this
        // generation (state.generation)
        // I am responsible for returning a population.  This could
        // be a new population that I created fresh, or I could modify
        // the existing population and return that.

        // for each of the islands that sends individuals

        for( int source = 0 ; source < exchangeInformation.length ; source++ )
        {
            // else, check whether the emigrants need to be sent
            if( ( state.generation >= exchangeInformation[source].offset ) && ( ( exchangeInformation[source].modulo == 0 ) ||
                    ( ( ( state.generation - exchangeInformation[source].offset ) % exchangeInformation[source].modulo ) == 0 ) ) )
            {
                // send the individuals!!!!
                // for each of the islands where we have to send individuals
                for( int x = 0 ; x < exchangeInformation[source].numDest ; x++ )
                {
                    int destination = exchangeInformation[source].destinations[x];
                    log(state, logID, false, "Sending immigrants from subpop " + source + " to subpop " + destination+"\n");
                    selectToImmigrate(state, source, destination);
                }
            }
        }
        log(state, logID, "\n");
        for(int i = 0; i < candidateImmigrants.size(); i++)
        {
            Individual[] inds = candidateImmigrants.get(i).values().toArray(new Individual[0]);
            ((SurrogatedHistoricMultiPopEvolutionState)state).surrogateEvaluate(inds, i);
            Arrays.sort(inds, Comparator.comparingDouble(r -> ((SuGPIndividual)r).getSurFit()));
            immigrants[i] = inds;
        }
        candidateImmigrants = new ArrayList<>();
        for (int i = 0; i < immigrants.length; i++) {
            candidateImmigrants.add(new HashMap<>());
        }

        return state.population;
    }

    public void selectToImmigrate(EvolutionState state, int from, int destination)
    {
        exchangeInformation[from].immigrantsSelectionMethod.prepareToProduce( state, from, 0 );

        int size = exchangeInformation[from].size * numMutations;
        Map<Integer, Individual> alreadyImmigrated = candidateImmigrants.get(destination);
        Map<Integer, Individual> currentImmigrants = new HashMap<>();

        SurrogatedHistoricMultiPopEvolutionState hstate = (SurrogatedHistoricMultiPopEvolutionState) state;
        Individual[] inds = state.population.subpops[from].individuals;
        hstate.surrogateEvaluate(inds, destination);

        int i = exchangeInformation[from].immigrantsSelectionMethod.produce( from, state, 0 );
        Individual ind = inds[i];
        int nMutated = 0;
        while(currentImmigrants.size() < size)
        {
            int[] ch = hstate.characterise(ind);
            int h = Arrays.hashCode(ch);
            if(!alreadyImmigrated.containsKey(h) && !currentImmigrants.containsKey(h))
            {
                nMutated = 0;
                currentImmigrants.put(h, ind);
                log(state, logID, false, ind.toString() + "\n");
                if(ind instanceof TLGPIndividual)
                {
                    double fitness = ind.fitness.fitness();
                    ((TLGPIndividual)ind).setOrigin(String.format("subpop.%d,%f", from, fitness));
                }
                i = exchangeInformation[from].immigrantsSelectionMethod.produce( from, state, 0 );
                ind = inds[i];
                continue;
            }
            ind = mutator.mutate(from, (GPIndividual)ind, state, 0);
            nMutated++;
            if(nMutated > numMutations)
            {
                nMutated = 0;
                i = exchangeInformation[from].immigrantsSelectionMethod.produce( from, state, 0 );
                ind = inds[i];
            }
        }

        alreadyImmigrated.putAll(currentImmigrants);
        nImmigrants[destination] += exchangeInformation[from].size;
        exchangeInformation[from].immigrantsSelectionMethod.finishProducing( state, from, 0 );
    }

    public Population postBreedingExchangePopulation(EvolutionState state)
    {
        // receiving individuals from other islands
        // This is where the immigrants are inserted into the target population.

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
            int imIndex = 0;
            for( int nTransferred = 0 ; nTransferred < nImmigrants[subTo]  && imIndex < immigrants[subTo].length;)
            {
                Individual immigrant = immigrants[subTo][imIndex++];
                log(state, logID, "Sending " + immigrant.toString() + " to " + subTo);
                boolean isNew = !hstate.IsSeenInCurrentPop(subTo, immigrant);
                if(!isNew)
                    log(state, logID, ",immiseen\n");
                else
                {
                    if (enableHistory)
                    {
                        isNew = hstate.isSeenIn(subTo, immigrant) <= 0;
                        if(!isNew)
                            log(state, logID, ",allpopseen\n");
                    }
                }

                if(isNew)
                {
                    log(state, logID, ",new" + "\n" );
                    log(state, logID, "Replacing," + state.population.subpops[subTo].individuals[ indices[nTransferred] ].toString() + "\n\n");
                    // send the individual
                    state.population.subpops[subTo].individuals[indices[nTransferred]] = immigrant;

                    // reset the evaluated flag (the individuals are not evaluated in the current island */
                    state.population.subpops[subTo].individuals[indices[nTransferred]].evaluated = false;
                    nTransferred++;
                }
                else
                    log(state, logID, "\n");
            }

            // reset the number of immigrants in the mailbox for the current subpopulation
            // this doesn't need another synchronization, because the thread is already synchronized
            nImmigrants[subTo] = 0;
        }

        return state.population;
    }
}
