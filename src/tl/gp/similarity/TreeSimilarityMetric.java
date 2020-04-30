package tl.gp.similarity;

import gphhucarp.decisionprocess.routingpolicy.GPRoutingPolicy;

/**
 * This interface encapsulates the distance measure between two GP individuals.
 */
public interface TreeSimilarityMetric
{
    /**
     * Measures how similar two given GP routing policies are to each other.
     * @param tree1 a GP routing policy.
     * @param tree2 another GP routing policy.
     * @return a real-valued number that indicates the level of distance. Lower values of this number indicate low
     * distance and higher-values indicate higher distance.
     */
    double distance(GPRoutingPolicy tree1, GPRoutingPolicy tree2);

    /**
     * This method is deprecated and the task of characterising routing policies is delegated to the
     * {@link tl.gp.characterisation.RuleCharacterisation} interface.
     */
//    @Deprecated
//    T characterise(GPRoutingPolicy tree);
}
