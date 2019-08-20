package tl.collections.tree;

public class Tree<E> {

    /**
     * The root node. This node does not logically belong to this tree as it
     * merely provides a way of having multiple "roots".
     */
    private final TreeNode<E> pseudoroot ;//= new TreeNode<>(null);

    /**
     * Returns the root node. It is <b>not</b> considered to belong to the
     * actual tree. We merely want to have a way of attaching nodes to it.
     *
     * @return the pseudoroot of this tree.
     */
    public TreeNode<E> getPseudoRoot()
    {
        return pseudoroot;
    }

    public Tree(E root)
    {
        pseudoroot = new TreeNode<>(root);
        pseudoroot.parent = null;
    }

    @Override
    public String toString()
    {
        /* TODO: Implement this. A good approach is to use the strategy pattern for converting the tree to string and use a
            default one. */
        return "";
    }
}