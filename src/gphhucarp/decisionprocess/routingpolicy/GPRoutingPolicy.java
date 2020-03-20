package gphhucarp.decisionprocess.routingpolicy;

import ec.gp.GPNode;
import ec.gp.GPTree;
import gphhucarp.core.Arc;
import gphhucarp.gp.UCARPPrimitiveSet;
import gphhucarp.representation.route.NodeSeqRoute;
import gphhucarp.decisionprocess.DecisionProcessState;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.RoutingPolicy;
import gphhucarp.decisionprocess.poolfilter.IdentityPoolFilter;
import gphhucarp.gp.CalcPriorityProblem;
import gputils.DoubleData;

import java.util.List;

/**
 * A GP-evolved routing policy.
 *
 * Created by gphhucarp on 30/08/17.
 */
public class GPRoutingPolicy extends RoutingPolicy {

    private GPTree gpTree;

    public GPRoutingPolicy(PoolFilter poolFilter, GPTree gpTree) {
        super(poolFilter);
        name = "\"GPRoutingPolicy\"";
        this.gpTree = gpTree;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public GPRoutingPolicy(GPTree gpTree) {
        this(new IdentityPoolFilter(), gpTree);
    }

    public GPTree getGPTree() {
        return gpTree;
    }

    public void setGPTree(GPTree gpTree) {
        this.gpTree = gpTree;
    }

    @Override
    public double priority(Arc candidate, NodeSeqRoute route, DecisionProcessState state) {
        CalcPriorityProblem calcPrioProb =
                new CalcPriorityProblem(candidate, route, state);

        DoubleData tmp = new DoubleData();
//        terminals(candidate, route, state);
        gpTree.child.eval(null, 0, tmp, null, null, calcPrioProb);

        return tmp.value;
    }

//    void terminals(Arc candidate, NodeSeqRoute route, DecisionProcessState state)
//    {
//        List<GPNode> terminals = UCARPPrimitiveSet.wholeTerminalSet().getList();
//        CalcPriorityProblem calcPrioProb = new CalcPriorityProblem(candidate, route, state);
//        DoubleData data = new DoubleData();
//        for(GPNode terminal : terminals)
//        {
//            terminal.eval(null, 0, data, null, null, calcPrioProb);
//            System.out.println(terminal.toString() + ": " + data.value);
//        }
//        System.out.println();
////        state.
//    }
}
