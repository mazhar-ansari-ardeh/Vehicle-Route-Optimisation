package tl.knowledge.ppt.pipe;

public interface IPIPELearner <T>
{
    void adaptTowards(PPTree tree, T learnFrom, int treeIndex);

    void initialize(ProbabilityVector vector);
}
