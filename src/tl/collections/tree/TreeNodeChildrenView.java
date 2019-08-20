package tl.collections.tree;


import java.util.*;

/**
 * This class provides a view over a tree nodes children. The client programmer
 * can manipulate it to his/her own liking.
 *
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Mar 27, 2018)
 * @param <E> the tree node element type.
 */
final class TreeNodeChildrenView<E> implements Set<TreeNode<E>>
{

    /**
     * The tree node that owns this view.
     */
    private final TreeNode<E> ownerTreeNode;

    /**
     * Constructs this view for the input tree node.
     *
     * @param ownerTreeNode the tree node this children view belongs to.
     */
    TreeNodeChildrenView(TreeNode<E> ownerTreeNode) {
        this.ownerTreeNode = ownerTreeNode;
    }

    /**
     * Returns the number of children in this view.
     *
     * @return the number of children.
     */
    @Override
    public int size() {
        return ownerTreeNode.children.size();
    }

    /**
     * Returns {@code true} only if this view has no child tree nodes.
     *
     * @return {@code true} if this view has no child tree nodes.
     */
    @Override
    public boolean isEmpty() {
        return ownerTreeNode.children.isEmpty();
    }

    /**
     * Returns {@code true} only if this tree node children view contains a
     * given tree node.
     *
     * @param o the query tree node.
     * @return {@code true} only if {@code o} is in this view.
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof TreeNode)) {
            return false;
        } else {
            return ownerTreeNode.children.contains(o);
        }
    }

    /**
     * Returns an iterator over this view's children.
     *
     * @return an iterator over this view's children.
     */
    @Override
    public Iterator<TreeNode<E>> iterator() {
        return ownerTreeNode.children.iterator();
    }

    /**
     * Adds a tree node to the children set of the owner tree node of this view. The method will not do anything if the
     * children set already contains this node. If the {@code treeNode} belongs to a parent, it will disconnected from it.
     * @param treeNode a node to be added
     * @return {@code true} if the node is added.
     */
    @Override
    public boolean add(TreeNode<E> treeNode) {
        Objects.requireNonNull(treeNode, "The input tree node is null.");
        checkInputTreeNodeIsNotPredecessorOfThisTreeNode(treeNode);

        // Return {@code false} whenever the input tree node is already in this
        // tree.
        if (ownerTreeNode.children.contains(treeNode)) {
            return false;
        }

        // If the input tree node belongs to a parent, disconnect it from it:
        if (treeNode.parent != null) {
            treeNode.parent.children.remove(treeNode);
        }

        // Connect the input tree node as the child of this view.
        ownerTreeNode.children.add(treeNode);
        treeNode.parent = ownerTreeNode;
        return true;
    }

    /**
     * Removes the given object from the children set of the owner node. The method will disconnect the removed object from
     * the owner node.
     * @param o The node to be removed.
     * @return {@code true} if the node is removed.
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof TreeNode)) {
            return false;
        }

        TreeNode<E> treeNode = (TreeNode<E>) o;
        treeNode.parent = null;
        return ownerTreeNode.children.remove(treeNode);
    }


    /**
     * Checks if the children set contains all the elements in the given collection.
     * @param c collection to be checked for containment in this set.
     *
     * @return {@code true} if this set contains all of the elements of the specified collection. The method returns
     * {@code false} if the given collection is {@code null}.
     */
    @Override
    public boolean containsAll(Collection<?> c)
    {
        if(c == null)
            return false;

        for (Object o : c)
        {
            if (!contains(o))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Adds all the elements in the given collection to the children of the owner node.
     * @param c the collection of elements to be added to the children set.
     * @return {@code true} if at least one element is added.
     */
    @Override
    public boolean addAll(Collection<? extends TreeNode<E>> c) {
        boolean modified = false;

        for (TreeNode<E> treeNode : c) {
            modified |= add(treeNode);
        }

        return modified;
    }

    /**
     * Retains only the elements in the children set that are contained in the specified collection (optional operation).
     * In other words, removes from the children set of the owner node all of its elements that are not contained in the
     * specified collection. If the specified collection is also a set, this operation effectively modifies this set so that
     * its value is the intersection of the two sets.
     * @param c – collection containing elements to be retained in this set. This parameter cannot be {@code null}.
     * @return {@code true} if this set changed as a result of the call.
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        int numberOfChildrenBefore = size();

        Set<?> collectionAsSet =
                (c instanceof HashSet) ? (Set<?>) c : new HashSet(c);

        Iterator<TreeNode<E>> iterator =
                ownerTreeNode.children.iterator();

        while (iterator.hasNext()) {
            TreeNode<E> currentTreeNode = iterator.next();

            if (!collectionAsSet.contains(currentTreeNode)) {
                iterator.remove();
            }
        }

        return size() < numberOfChildrenBefore;
    }

    /**
     * Removes all the elements in the {@code c} that are present in the children of the owner node.
     * @param c the collection of items to be removed. This object cannot be {@code null}.
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return ownerTreeNode.children.removeAll(c);
    }

    /**
     * Clears the children set of all the elements it contains.
     */
    @Override
    public void clear()
    {
        ownerTreeNode.children.clear();
    }

    /**
     * Returns an array containing all of the elements in the children set of the owner node. This method returns the
     * elements in the same order that they were added. The returned array will be "safe" in that no references to it are
     * maintained by owner node. (In other words, this method allocates a new array). The caller is thus free to modify the
     * returned array. <br><br>
     *
     * This method acts as bridge between array-based and collection-based APIs.
     * @return an array containing all the elements in the children set
     */
    @Override
    public Object[] toArray()
    {
        return ownerTreeNode.children.toArray();
//        throw new UnsupportedOperationException();
    }

    /**
     * Returns an array containing all of the elements in children set of the owner node; the runtime type of the returned
     * array is that of the specified array. If the children set fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the specified array and the size of this set.
     *
     * <p>If this set fits in the specified array with room to spare (i.e., the array has more elements than this set), the
     * element in the array immediately following the end of the set is set to <tt>null</tt>. (This is useful in determining
     * the length of this set <i>only</i> if the caller knows that this set does not contain any null elements.)
     *
     * <p>This method returns the elements in the same order that they were added.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between array-based and collection-based APIs.
     * Further, this method allows precise control over the runtime type of the output array, and may, under certain
     * circumstances, be used to save allocation costs.
     *
     *
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param a the array into which the elements of this set are to be
     *        stored, if it is big enough; otherwise, a new array of the same
     *        runtime type is allocated for this purpose.
     * @return an array containing all the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in this
     *         set
     * @throws NullPointerException if the specified array is null
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return ownerTreeNode.children.toArray(a);
        // throw new UnsupportedOperationException();
    }

    /**
     * Checks that the input tree node is not a predecessor of itself.
     *
     * @param treeNode the tree node to check.
     */
    private void checkInputTreeNodeIsNotPredecessorOfThisTreeNode(
            TreeNode<E> treeNode) {
        TreeNode<E> currentTreeNode = ownerTreeNode;

        while (currentTreeNode != null)
        {
            if (currentTreeNode == treeNode)
            {
                throw new IllegalStateException("Trying to create a cycle in this tree.");
            }

            currentTreeNode = currentTreeNode.parent;
        }
    }
}
