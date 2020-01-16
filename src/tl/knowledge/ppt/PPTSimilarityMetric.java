package tl.knowledge.ppt;

import ec.gp.GPNode;
import tl.knowledge.ppt.pipe.PPTree;


/**
 * This interface encapsulates the similarity measure between a PPT and a GP individual.
 */
public interface PPTSimilarityMetric
{
    /**
     * Measures how similar a given GP individual, {@code tree} is to a given PPT. A good similarity measure should give a
     * high score to GP individuals that are very likely to be generated from the given PPT.
     * @param ppt a {@code PPTree} object.
     * @param tree a GP tree to be measure its similarity to the given PPT.
     * @return a real-valued number that indicates the level of similarity. Lower values of this number indicate low
     * similarity and higher-values indicate higher similarity.
     */
    double similarity(PPTree ppt, GPNode tree);
}
