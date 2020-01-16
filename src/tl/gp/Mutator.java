package tl.gp;

import ec.EvolutionState;
import ec.gp.*;

import java.io.Serializable;

/**
 * MutationPipeline is a GPBreedingPipeline which
 * implements a strongly-typed version of the
 * "Point Mutation" operator as described in Koza I.
 * Actually, that's not quite true.  Koza doesn't have any tree depth restrictions
 * on his mutation operator.  This one does -- if the tree gets deeper than
 * the maximum tree depth, then the new subtree is rejected and another one is
 * tried.  Similar to how the Crosssover operator is implemented.
 *
 * <p>Mutated trees are restricted to being <tt>maxdepth</tt> depth at
 * most and at most <tt>maxsize</tt> number of nodes.  If in
 * <tt>tries</tt> attemptes, the pipeline cannot come up with a
 * mutated tree within the depth limit, then it simply copies the
 * original individual wholesale with no mutation.
 *
 * <p>One additional feature: if <tt>equal</tt> is true, then MutationPipeline
 * will attempt to replace the subtree with a tree of approximately equal size.
 * How this is done exactly, and how close it is, is entirely up to the pipeline's
 * tree builder -- for example, Grow/Full/HalfBuilder don't support this at all, while
 * RandomBranch will replace it with a tree of the same size or "slightly smaller"
 * as described in the algorithm.

 <p><b>Typical Number of Individuals Produced Per <tt>produce(...)</tt> call</b><br>
 ...as many as the child produces

 <p><b>Number of Sources</b><br>
 1

 <p><b>Parameters</b><br>
 <table>
 <tr><td valign=top><i>base</i>.<tt>tries</tt><br>
 <font size=-1>int &gt;= 1</font></td>
 <td valign=top>(number of times to try finding valid pairs of nodes)</td></tr>

 <tr><td valign=top><i>base</i>.<tt>maxdepth</tt><br>
 <font size=-1>int &gt;= 1</font></td>
 <td valign=top>(maximum valid depth of a crossed-over subtree)</td></tr>

 <tr><td valign=top><i>base</i>.<tt>maxsize</tt><br>
 <font size=-1>int &gt;= 1</font></td>
 <td valign=top>(maximum valid size, in nodes, of a crossed-over subtree)</td></tr>

 <tr><td valign=top><i>base</i>.<tt>ns</tt><br>
 <font size=-1>classname, inherits and != GPNodeSelector</font></td>
 <td valign=top>(GPNodeSelector for tree)</td></tr>

 <tr><td valign=top><i>base</i>.<tt>build</tt>.0<br>
 <font size=-1>classname, inherits and != GPNodeBuilder</font></td>
 <td valign=top>(GPNodeBuilder for new subtree)</td></tr>

 <tr><td valign=top><tt>equal</tt><br>
 <font size=-1>bool = <tt>true</tt> or <tt>false</tt> (default)</td>
 <td valign=top>(do we attempt to replace the subtree with a new one of roughly the same size?)</td></tr>

 <tr><td valign=top><i>base</i>.<tt>tree.0</tt><br>
 <font size=-1>0 &lt; int &lt; (num trees in individuals), if exists</font></td>
 <td valign=top>(tree chosen for mutation; if parameter doesn't exist, tree is picked at random)</td></tr>

 </table>

 <p><b>Default Base</b><br>
 gp.koza.mutate

 <p><b>Parameter bases</b><br>
 <table>

 <tr><td valign=top><i>base</i>.<tt>ns</tt><br>
 <td>nodeselect</td></tr>

 <tr><td valign=top><i>base</i>.<tt>build</tt><br>
 <td>builder</td></tr>

 </table>



 * @author Sean Luke
 * @version 1.0
 */

public class Mutator implements Serializable // extends GPBreedingPipeline
{
    private static final long serialVersionUID = 1;

    public static final String P_NUM_TRIES = "tries";
    public static final String P_MAXDEPTH = "maxdepth";
    public static final String P_MAXSIZE = "maxsize";
//    public static final String P_MUTATION = "mutate";
//    public static final String P_BUILDER = "build";
//    public static final String P_EQUALSIZE = "equal";
    public static final int NUM_SOURCES = 1;
    public static final int NO_SIZE_LIMIT = -1;

    /** How the pipeline chooses a subtree to mutate */
    public GPNodeSelector nodeselect;

    /** How the pipeline builds a new subtree */
    public GPNodeBuilder builder;

    /** The number of times the pipeline tries to build a valid mutated
     tree before it gives up and just passes on the original */
    public int numTries;

    /** The maximum depth of a mutated tree */
    public int maxDepth;

    /** The largest tree (measured as a nodecount) the pipeline is allowed to form. */
    public int maxSize;

    /** Do we try to replace the subtree with another of the same size? */
    public boolean equalSize;


    /**
     * Which tree to mutate.
     */
    public int tree;

    /**
     *
     * @param state The state of the evolution.
     * @param selector this parameter must be setup before sending to this constructor.
     * @param nodeBuilder this parameter must be setup before sending to this constructor.
     */
    public Mutator(final EvolutionState state, GPNodeSelector selector, GPNodeBuilder nodeBuilder, int numberOfTries,
                   int maxTreeDepth, int maxTreeSize, boolean equalTreeSize)
    {
        nodeselect = selector;
        builder = nodeBuilder;
        numTries = numberOfTries;
        if (numTries ==0)
            state.output.fatal("Invalid number of tries (it must be >= 1): " + numTries);

        maxDepth = maxTreeDepth;
        if (maxDepth==0)
            state.output.fatal("Invalid maximum depth (it must be >= 1): " + maxDepth);

        maxSize = maxTreeSize;
//        if (maxSize < 1)
//            state.output.fatal("Maximum tree size, if defined, must be >= 1");

        equalSize = equalTreeSize;
    }


    /** Returns true if inner1 can feasibly be swapped into inner2's position */

    private boolean verifyPoints(GPNode inner1, GPNode inner2)
    {
        // We know they're swap-compatible since we generated inner1
        // to be exactly that.  So don't bother.

        // next check to see if inner1 can fit in inner2's spot
        if (inner1.depth()+inner2.atDepth() > maxDepth) return false;

        // check for size
        if (maxSize != NO_SIZE_LIMIT)
        {
            // first easy check
            int inner1size = inner1.numNodes(GPNode.NODESEARCH_ALL);
            int inner2size = inner2.numNodes(GPNode.NODESEARCH_ALL);
            if (inner1size > inner2size)  // need to demo further
            {
                // let's keep on going for the more complex demo
                GPNode root2 = ((GPTree)(inner2.rootParent())).child;
                int root2size = root2.numNodes(GPNode.NODESEARCH_ALL);
                if (root2size - inner2size + inner1size > maxSize)  // take root2, remove inner2 and swap in inner1.  Is it still small enough?
                    return false;
            }
        }

        // checks done!
        return true;
    }

    public GPNode mutate(int subpopulation, GPNode toMutate, final EvolutionState state, int thread)
    {
        GPIndividual retval = mutate(subpopulation, GPIndividualUtils.asGPIndividual(toMutate), state, thread, 0);
        return GPIndividualUtils.stripRoots(retval).get(0);
    }


    public GPIndividual mutate(int subpopulation, GPIndividual toMutate, final EvolutionState state, int thread, int tree)
    {
        GPInitializer initializer = ((GPInitializer)state.initializer);

        // now let's mutate 'em

        if (tree!=-1 && (tree<0 || tree >= toMutate.trees.length))
            // uh oh
            state.output.fatal("GP Mutation Pipeline attempted to fix tree.0 to a value which was out of bounds of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may be negative or greater than the number of trees in an individual");


        int t;
        // pick random tree
        if (tree==-1)
            if (toMutate.trees.length>1)
                t = state.random[thread].nextInt(toMutate.trees.length);
            else
                t = 0;
        else
            t = tree;

        // validity result...
        boolean res = false;

        // prepare the nodeselector
        nodeselect.reset();

        // pick a node

        GPNode p1=null;  // the node we pick
        GPNode p2=null;

        for(int x=0;x<numTries;x++)
        {
            // pick a node in individual 1
            p1 = nodeselect.pickNode(state,subpopulation,thread, toMutate, toMutate.trees[t]);

            // generate a tree swap-compatible with p1's position


            int size = GPNodeBuilder.NOSIZEGIVEN;
            if (equalSize) size = p1.numNodes(GPNode.NODESEARCH_ALL);

            p2 = builder.newRootedTree(state, p1.parentType(initializer), thread, p1.parent,
                    toMutate.trees[t].constraints(initializer).functionset, p1.argposition, size);

            // check for depth and swap-compatibility limits
            res = verifyPoints(p2,p1);  // p2 can fit in p1's spot  -- the order is important!

            // did we get something that had both nodes verified?
            if (res) break;
        }


        GPIndividual retval = toMutate.lightClone();

        // Fill in various tree information that didn't get filled in there
        retval.trees = new GPTree[toMutate.trees.length];

        // at this point, p1 or p2, or both, may be null.
        // If not, swap one in.  Else just copy the parent.
        for(int x=0;x<retval.trees.length;x++)
        {
            if (x==t && res)  // we've got a tree with a kicking cross position!
            {
                retval.trees[x] = (GPTree)(toMutate.trees[x].lightClone());
                retval.trees[x].owner = retval;
                retval.trees[x].child = toMutate.trees[x].child.cloneReplacingNoSubclone(p2,p1);
                retval.trees[x].child.parent = retval.trees[x];
                retval.trees[x].child.argposition = 0;
                retval.evaluated = false;
            } // it's changed
            else
            {
                retval.trees[x] = (GPTree)(toMutate.trees[x].lightClone());
                retval.trees[x].owner = retval;
                retval.trees[x].child = (GPNode)(toMutate.trees[x].child.clone());
                retval.trees[x].child.parent = retval.trees[x];
                retval.trees[x].child.argposition = 0;
            }
        }

        return retval;
    }
}
