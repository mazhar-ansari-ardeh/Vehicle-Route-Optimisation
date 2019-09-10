package tl.knowledge.ppt.pipe;

import java.io.Serializable;

public interface IPIPELearner <T> extends Serializable
{
    void adaptTowards(PPTree tree, T learnFrom, int treeIndex);

    void initialize(ProbabilityVector vector);
}
