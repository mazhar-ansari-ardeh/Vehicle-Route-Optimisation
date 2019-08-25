package tl.collections.tree;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Stack;

public class Tree<E> {

    /**
     * The root node.
     */
    protected TreeNode<E> root;//= new TreeNode<>(null);

    /**
     * Returns the root node. It is <b>not</b> considered to belong to the
     * actual tree. We merely want to have a way of attaching nodes to it.
     *
     * @return the root of this tree.
     */
    public TreeNode<E> getRoot()
    {
        return root;
    }

    /**
     * The default constructor. This constructor will set the root node to {@code null}.
     */
    public Tree()
    {
        this.root = null;
    }

    public Tree(E root)
    {
        this.root = new TreeNode<>(root);
        this.root.parent = null;
    }

    @Override
    public String toString()
    {
        /* TODO: Implement this. A good approach is to use the strategy pattern for converting the tree to string and use a
            default one. */
        return "";
    }

    public Iterator<E> iterator()
    {
        return new PostFixTreeIterator(root);
    }

    class PreFixTreeIterator implements Iterator<E>
    {
        TreeNode<E> cursor;
        Stack<Iterator<TreeNode<E>>> stack = new Stack<>();

        public PreFixTreeIterator(TreeNode<E> root)
        {
            this.cursor = root;
        }

        @Override
        public boolean hasNext()
        {
            return cursor != null;
        }

        @Override
        public E next()
        {
            if(cursor == null)
                return null;

//            System.out.println(cursor);
            E retval = cursor.getElement();
            Iterator<TreeNode<E>> it = cursor.getChildren().iterator();
            if(it.hasNext())
            {
                cursor = it.next();
                stack.push(it);
            }
            else
            {
                it = stack.pop();
                while(!it.hasNext() && !stack.isEmpty())
                {
                    it = stack.pop();
                }
                if(stack.isEmpty() && !it.hasNext()) // Finished iterating the tree
                {
                    cursor = null;
                    return retval;
                }
                cursor = it.next();
                stack.push(it);
            }
            return retval;
        }
    }

    class PostFixTreeIterator implements Iterator<E>
    {
        TreeNode<E> cursor;
        Stack<Iterator<TreeNode<E>>> stack = new Stack<>();

        public PostFixTreeIterator(TreeNode<E> root)
        {
            LinkedHashSet<TreeNode<E>> rootSet = new LinkedHashSet<>();
            rootSet.add(root);
            Iterator<TreeNode<E>> it = rootSet.iterator();
            this.cursor = it.next();
            stack.push(it);
        }

        @Override
        public boolean hasNext()
        {
            return cursor != null;
        }

        /**
         * A simple flag to remember whether in the next iterate, the tree should be traversed down from the cursor or the
         * current value under the cursor should be used. This value is {@code false} when it is time to use the root of the
         * subtree or tree that cursor points to.
         */
        private boolean goDeep = true;

        @Override
        public E next()
        {
            if(cursor == null)
                return null;

            E retval; // = cursor.getElement();

            Iterator<TreeNode<E>> it = cursor.getChildren().iterator();
            while (goDeep && it.hasNext())
            {
                stack.push(it);
                cursor = it.next();
                it = cursor.getChildren().iterator();
            }
            retval = cursor.getElement();
            it = stack.pop();
            if(!it.hasNext())
            {
                cursor = cursor.parent;
                goDeep = false;
            }
            else
            {
                goDeep = true;
                cursor = it.next();
                stack.push(it);
            }

            return retval;

        }
    }
}

