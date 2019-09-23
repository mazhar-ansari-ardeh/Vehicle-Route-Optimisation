package tl.gp;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.*;
import ec.util.Parameter;
import tl.gp.hash.AlgebraicHashCalculator;
import tl.gp.hash.HashCalculator;
import tl.gp.simplification.AlgebraicTreeSimplifier;
import tl.gp.simplification.TreeSimplifier;
import tl.knowledge.codefragment.CodeFragmentKI;

import java.util.ArrayList;

public class MutatingSubtreeBuilder extends SimpleCodeFragmentBuilder
{
    /**
     * If {@code true}, the algorithm will simplify the extracted trees before transferring. Mutation will be performed on
     * the simplified trees.
     */
    public final String P_SIMPLIFY = "simplify";
    private boolean simplify;

    TreeSimplifier simplifier = null;

    /**
     * The percentage of initial population that is created from extracted knowledge. The value must
     * be in range (0, 1].
     */
    public static final String P_TARGET_PERCENT = "target-percent";
    private double targetPercent;

    /**
     * Number of individuals that are created by performing a mutation operator is on a transferred individual.
     * A zero or negative value for this parameter indicates that the mutation should be disabled.
     */
    public static final String P_NUM_MUTATE = "num-mutated";
    private int numMutate;

    /**
     * The last item that was loaded from source domain and transferred to the target domain.
     */
    private GPNode lastLoadedTransfer = null;

    /**
     * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
     * population that is transferred from extracted knowledge. The value must be in range (0, 1].
     */

    public final String P_NODESELECTOR = "ns";
    private GPNodeSelector nodeselect;

    private ArrayList<CodeFragmentKI> niche = new ArrayList<>();

    /**
     * The niche capacity. If the niche radius is a negative value, the value of capacity will be redundant.
     */
    public static final String p_NICHE_CAPACITY = "niche-capacity";
    private int nicheCapacity = 2;

    /**
     * The radius of the niching mechanism. A negative value for this parameter will disable niching.
     */
    public static final String P_NICHE_RADIUS = "niche-radius";
    private double nicheRadius;

    /**
     * Keeps track of the number of items that are to be created from mutating a transferred item. This value is reset to
     * {@code numMutate} when it hits zero.
     */
    int numToMutate;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        Parameter def = defaultBase();
        Parameter p = base.push(P_NODESELECTOR).push(""+0);
        Parameter d = def.push(P_NODESELECTOR).push(""+0);

        nodeselect = (GPNodeSelector) (state.parameters.getInstanceForParameter(p,d, GPNodeSelector.class));
        nodeselect.setup(state,p);

        p = base.push(P_NUM_MUTATE);
        numMutate = state.parameters.getInt(p, null);
        numToMutate = numMutate;
        state.output.warning("Number of mutations: " + numMutate);

        p = base.push(P_TARGET_PERCENT);
        targetPercent = state.parameters.getDouble(p, null);
        if(targetPercent <= 0 || targetPercent > 1)
            state.output.fatal("Invalid transfer percent. Transfer percent must be in (0, 1]: " + targetPercent);
        else
            state.output.warning("Transfer percent: " + targetPercent);

        p = base.push(P_SIMPLIFY);
        simplify = state.parameters.getBoolean(p, null, true);
        state.output.warning("Simplify: " + simplify);
        if(simplify)
        {
            HashCalculator hs = new AlgebraicHashCalculator(state, 0, 1000077157);
            simplifier = new AlgebraicTreeSimplifier(hs);
        }

        p = base.push(P_NICHE_RADIUS);
        nicheRadius = state.parameters.getDouble(p, null);
        state.output.warning("Niche radius: " + nicheRadius);

        if(nicheRadius >= 0)
        {
            p = base.push(p_NICHE_CAPACITY);
            nicheCapacity = state.parameters.getInt(p, null);
            if(nicheCapacity <= 0)
                state.output.warning("Niche capacity must be a positive value: " + nicheCapacity);
            else
                state.output.warning("Niche capacity: " + nicheCapacity);
        }
    }

    private GPNode mutate(GPNode node, int subpopulation, final EvolutionState state, final int thread, GPFunctionSet set)
    {
        assert node != null;

        GPIndividual ind = GPIndividualUtils.asGPIndividual(node);
        return GPIndividualUtils.stripRoots(mutate(subpopulation, ind, state, thread, set)).get(0);
    }

    private boolean verifyPoints(GPNode inner1, GPNode inner2)
    {
        // We know they're swap-compatible since we generated inner1
        // to be exactly that.  So don't bother.

        // next check to see if inner1 can fit in inner2's spot
        if (inner1.depth()+inner2.atDepth() > maxDepth) return false;

        final int NO_SIZE_LIMIT = -1;
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

    private GPIndividual mutate(int subpopulation, Individual ind, final EvolutionState state, final int thread,
                                GPFunctionSet set)
    {
        // grab individuals from our source and stick 'em right into inds.
        // we'll modify them from there
//        int n = sources[0].produce(min,max,start,subpopulation,inds,state,thread);

        // should we bother?
        GPInitializer initializer = ((GPInitializer)state.initializer);

        // TODOx: Check if 'i' is modified or not.
        GPIndividual i = (GPIndividual)ind;

        // validity result...
        boolean res = false;

        // prepare the nodeselector
        nodeselect.reset();

        // pick a node

        GPNode p1=null;  // the node we pick
        GPNode p2=null;

        int numTries = 3;
        for(int x=0;x<numTries;x++)
        {
            // pick a node in individual 1
            p1 = nodeselect.pickNode(state,subpopulation,thread,i,i.trees[0]);

            // generate a tree swap-compatible with p1's position

            p2 = newRootedTreeGF(state, p1.parentType(initializer), thread, p1.parent, set, p1.argposition);

            // check for depth and swap-compatibility limits
            res = verifyPoints(p2,p1);  // p2 can fit in p1's spot  -- the order is important!

            // did we get something that had both nodes verified?
            if (res) break;
        }

        GPIndividual j;

        j = i.lightClone();

        // Fill in various tree information that didn't get filled in there
        j.trees = new GPTree[i.trees.length];

        // at this point, p1 or p2, or both, may be null.
        // If not, swap one in.  Else just copy the parent.
        for(int x=0;x<j.trees.length;x++)
        {
            if (res)  // we've got a tree with a kicking cross position!
            {
                j.trees[x] = (i.trees[x].lightClone());
                j.trees[x].owner = j;
                j.trees[x].child = i.trees[x].child.cloneReplacingNoSubclone(p2,p1);
                j.trees[x].child.parent = j.trees[x];
                j.trees[x].child.argposition = 0;
                j.evaluated = false;
            } // it's changed
            else
            {
                j.trees[x] = (i.trees[x].lightClone());
                j.trees[x].owner = j;
                j.trees[x].child = (GPNode)(i.trees[x].child.clone());
                j.trees[x].child.parent = j.trees[x];
                j.trees[x].child.argposition = 0;
            }
        }

        return j;
    }

    // Next Code Fragment
    private GPNode nextCF(EvolutionState state, GPNodeParent parent)
    {
        CodeFragmentKI cf = null;
        boolean ready = false;
        while (!ready)
        {
            cf = (CodeFragmentKI) extractor.getNext();
            if (cf == null)
            {
                log(state, null, knowledgeSuccessLogID);
                return null;
            }

            if (niche.size() == 0)
            {
                niche.add(cf);
                ready = true;
            }
            else
            {
                CodeFragmentKI center = niche.get(0);
                if (Math.abs(center.getSourceFitness() - cf.getSourceFitness()) <= nicheRadius)
                {
                    if (niche.size() < nicheCapacity)
                    {
                        niche.add(cf);
                        ready = true;
                    }
                    else
                        log(state, knowledgeSuccessLogID, "CF ignored due to niching: " + cf.toString() + "\n");
                }
                else // New niche
                {
                    niche.clear();
                    niche.add(cf);
                    ready = true;
                }
            }
        }

        GPNode node = cf.getItem();

        node.parent = parent;
        lastLoadedTransfer = node;
        return node;
    }

    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
                                final GPNodeParent parent, final GPFunctionSet set, final int argposition,
                                final int requestedSize)
    {
        int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        int numToTransfer = (int) Math.round(popSize * targetPercent);

        if(cfCounter >= numToTransfer)
            return newRootedTreeGF(state, type, thread, parent, set, argposition);

        if(lastLoadedTransfer == null)
        {
            GPNode n = nextCF(state, parent);

            if (n != null)
            {
                cfCounter++;
                log(state, knowledgeSuccessLogID, cfCounter + ": \t" + new CodeFragmentKI(n).toString() + "\n");
                if(simplify)
                {
                    GPIndividual ind = GPIndividualUtils.asGPIndividual(n);
                    boolean simplified = simplifier.simplifyTree(state, ind);
                    n = GPIndividualUtils.stripRoots(ind).get(0);
                    if(simplified)
                        log(state, knowledgeSuccessLogID, cfCounter + " (simplified): \t" + new CodeFragmentKI(n).toString() + "\n");
                }
                n.parent = parent;
                lastLoadedTransfer = n;
                return n;
            }
            else
                log(state, null, knowledgeSuccessLogID);
        }
        else
        {
            GPNode mutated = mutate(lastLoadedTransfer, 0, state, thread, set);
            mutated.parent = parent;
            numToMutate--;
            cfCounter++;
            log(state, knowledgeSuccessLogID, cfCounter + "(mutated): \t" + mutated.makeCTree(false, true, true) + "\n");
            if(numToMutate <= 0)
            {
                lastLoadedTransfer = null;
                numToMutate = numMutate;
            }
            return mutated;
        }
        return newRootedTreeGF(state, type, thread, parent, set, argposition);
    }

    private GPNode newRootedTreeGF(final EvolutionState state, final GPType type, final int thread,
                                   final GPNodeParent parent, final GPFunctionSet set, final int argposition)
    {
        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1)
                    + minDepth,type,thread,parent,argposition,set);
        else
            return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1)
                    + minDepth,type,thread,parent,argposition,set);
    }
}
