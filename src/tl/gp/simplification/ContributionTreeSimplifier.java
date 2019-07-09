package tl.gp.simplification;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleProblemForm;
import gputils.terminal.DoubleERC;
import tl.gp.GPIndividualUtils;
import tl.gp.TLGPIndividual;
import tl.gp.TreeSlicer;

public class ContributionTreeSimplifier extends TreeSimplifier
{
//    protected boolean simplify(EvolutionState state, GPIndividual ind) {
//        return false;
//    }


    private static boolean simplifyWithContrib(EvolutionState state, GPIndividual ind, GPNode root)
    {
        assert state != null;
        assert ind != null;

        if(root == null || root.children == null || root.children.length == 0)
            return false;

        boolean changed = false;
        for(int i = 0; i < root.children.length; i++)
        {
            double subtreeContrib = TreeSlicer.getSubtreeContrib(state, ind, root.children[i]);
            // Contribution is measured as 'fitnessWithSubtree - fitnessWithoutSubtree' for which a positive value indicates
            // that the tree has a lower cost without the subtree.
            if(subtreeContrib >= 0)
            {
                DoubleERC constNode = new DoubleERC();
                constNode.value = 1;
                GPIndividualUtils.replace(root.children[i], constNode);
                changed = true;
            }
            ((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, 0, 0);
        }

        for(int i = 0; i < root.children.length; i++)
        {
            changed |= simplifyWithContrib(state, ind, root.children[i]);
        }

        return changed;
    }

    @Override
    protected boolean simplify(EvolutionState state, GPIndividual ind)
    {
        if(state == null)
            throw new NullPointerException("State object cannot be null");
        if(ind == null)
            throw new NullPointerException("Input individual cannot be null");

        if(ind instanceof TLGPIndividual)
        {
            if(!((TLGPIndividual) ind).isTested())
            {
                state.output.fatal("GPIndividual is not evaluated on test scenario");
            }
        }
        else
            state.output.warning("GPIndividual is not of type TLGPIndividual");

        boolean changed = false;
        for(GPTree tree : ind.trees)
        {
            GPNode root = tree.child;
            changed |= simplifyWithContrib(state, ind, root);
        }

        return changed;
    }

}
