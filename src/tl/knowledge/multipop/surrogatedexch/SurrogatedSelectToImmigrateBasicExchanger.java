package tl.knowledge.multipop.surrogatedexch;

import ec.EvolutionState;
import ec.Individual;
import tl.knowledge.multipop.ImmigrantAwareBasicExchanger;

/**
 * This class is an extension of {@link ImmigrantAwareBasicExchanger} in which the {@code this.selectToImmigrate} method
 * is overridden to evaluate the source population with a surrogate model of the target population. The method requires
 * the {@link SurrogatedHistoricMultiPopEvolutionState} evolution state object, which in turn, requires
 * the {@code SuGPIndividual} class of individuals. The method updates the surrogate fitness of the individuals (and not
 * the actual fitness). Selection of the immigrants relies on implementation of the parent which in turn, relies on the
 * the selection method that the class is configured to use. In order to use the surrogate fitness for selection, the
 * class should be configured to use the {@link SurrogateTournamentSelection} class.
 */
public class SurrogatedSelectToImmigrateBasicExchanger extends ImmigrantAwareBasicExchanger
{

    protected void selectToImmigrate(EvolutionState state, int from, int destination)
    {
        SurrogatedHistoricMultiPopEvolutionState hstate = (SurrogatedHistoricMultiPopEvolutionState) state;
        Individual[] inds = state.population.subpops[from].individuals;
        hstate.surrogateEvaluate(inds, destination);

        // For this to work, the exchanger should be configured to use SurrogateTournamentSelection.
        super.selectToImmigrate(state, from, destination);
    }
}
