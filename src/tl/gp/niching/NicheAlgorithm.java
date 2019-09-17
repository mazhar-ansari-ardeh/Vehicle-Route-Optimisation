package tl.gp.niching;

import ec.gp.GPIndividual;

/**
 * A high-level interface that defines the interface of niching algorithms.
 */
public interface NicheAlgorithm
{
    /**
     * Applies the niching algorithm defined by the implementing class to a set of individuals and returns the result.
     * @param population a set of individuals that will be the niching algorithm will be performed on.
     * @return the set of individuals that are obtained after the niching algorithm is applied.
     */
    GPIndividual[] applyNiche(GPIndividual[] population);
}
