package tl.knowledge.multipop.surrogatedexch;

import ec.EvolutionState;
import ec.Individual;
import ec.select.TournamentSelection;
import ec.util.Parameter;
import tl.knowledge.surrogate.SuGPIndividual;

public class SurrogateTournamentSelection extends TournamentSelection
{
    public static final String P_DEFAULT_BASE = "surrogate-tselect";

    @Override
    public boolean betterThan(Individual first, Individual second, int subpopulation, EvolutionState state, int thread)
    {
        return ((SuGPIndividual)first).getSurFit() < ((SuGPIndividual)(second)).getSurFit();
    }

    @Override
    public Parameter defaultBase() {
        return new Parameter(P_DEFAULT_BASE);
    }
}
