package tl.gp.simplification;

import java.io.IOException;
import java.util.ArrayList;

import ec.Evaluator;
import ec.EvolutionState;
import ec.Evolve;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.util.Parameter;
import ec.util.ParameterDatabase;
import gphhucarp.gp.terminal.feature.RemainingCapacity;
import gphhucarp.gp.terminal.feature.ServeCost;
import gputils.function.Add;
import gputils.function.Sub;
import gputils.terminal.DoubleERC;
import gputils.terminal.TerminalERC;
import gputils.terminal.TerminalERCUniform;
import tl.gp.PopulationUtils;
import tl.gp.hash.AlgebraicHashCalculator;
import tl.gp.hash.HashCalculator;

public class AlgebraicTreeSimplifier extends TreeSimplifier
{
    private HashCalculator hashCalculator;

	public AlgebraicTreeSimplifier(HashCalculator hasher)
	{
		if(hasher == null)
			throw new IllegalArgumentException("Hash calculator is null");

		hashCalculator = hasher;
	}

	private static EvolutionState loadECJ(String paramFileNamePath, String... ecjParams)
	{
		ArrayList<String> params = new ArrayList<>();
		params.add("-file");
		params.add(paramFileNamePath);
		for(String param : ecjParams)
		{
			params.add("-p");
			params.add(param);
		}
		String[] processedParams = new String[params.size()];
		params.toArray(processedParams);
		ParameterDatabase parameters = Evolve.loadParameterDatabase(processedParams);

		EvolutionState retval = Evolve.initialize(parameters, 0);

		Parameter p;

		// setup the evaluator, essentially the test evaluation model
		p = new Parameter(EvolutionState.P_EVALUATOR);
		retval.evaluator = (Evaluator)
				(parameters.getInstanceForParameter(p, null, Evaluator.class));
		retval.evaluator.setup(retval, p);

		p = new Parameter("eval.problem.eval-model.instances.0.samples");
		int samples = retval.parameters.getInt(p, null);
//		if(samples < 100)
//			retval.output.fatal("Sample size is too small: " + samples);
//		else
//			retval.output.warning("Sample size in AnalyzeTerminals: " + samples);

		return retval;
	}

	private static boolean isDoubleERC(GPNode node, double value)
	{
        return (node instanceof TerminalERCUniform)
                && ((TerminalERCUniform) node).getTerminal().name().equals("ERC")
                && ((DoubleERC) ((TerminalERCUniform) node).getTerminal()).value == value;
    }


	private static boolean applyConstRule(GPNode tree)
	{
		if(tree.children == null || tree.children.length == 0)
			return false;
		GPNode ch1 = tree.children[0];
		GPNode ch2 = tree.children[1];

		if( !(ch1 instanceof TerminalERC) || !(ch2 instanceof TerminalERC))
			return false;

		if(    !((TerminalERCUniform)ch1).getTerminal().name().equals("ERC")
				|| !((TerminalERCUniform)ch2).getTerminal().name().equals("ERC"))
			return false;

		DoubleERC dch1 = ((DoubleERC)((TerminalERCUniform)ch1).getTerminal());
		DoubleERC dch2 = ((DoubleERC)((TerminalERCUniform)ch2).getTerminal());
		String operation = tree.toString();
		double value1 = dch1.value;
		double value2 = dch2.value;

		DoubleERC erc = new DoubleERC();
        switch (operation) {
            case "+":
                erc.value = value1 + value2;
                break;
            case "-":
                erc.value = value1 - value2;
                break;
            case "*":
                erc.value = value1 * value2;
                break;
            case "/":
                erc.value = value1 / (value2 == 0 ? 1 : value2);
                break;
            case "min":
                erc.value = Math.min(value1, value2);
                break;
            case "max":
                erc.value = Math.max(value1, value2);
                break;
            default:
                throw new RuntimeException("Unknown operation to simplify.");
        }

		Object oparent = tree.parent;
		tree.parent = null;

		TerminalERCUniform res = new TerminalERCUniform();
		res.children = new GPNode[0];
		res.setTerminal(erc);
		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
//			treeParent = parent;
		}

		return true;
	}

	private boolean applyMinMaxEqualRule(GPNode tree)
	{
		if(!tree.toString().equals("min") && !tree.toString().equals("max"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if(hashCalculator.hashOfTree(tree.children[0]) != hashCalculator.hashOfTree(tree.children[1]))
			return false;

		GPNode res = tree.children[0];
		Object oparent = tree.parent;

		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else
			throw new RuntimeException("Unknown parent type: " + oparent);

		return true;
	}

	private boolean applySelfDivSubRule(GPNode tree)
	{
		if(!tree.toString().equals("/") && !tree.toString().equals("-"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if(hashCalculator.hashOfTree(tree.children[0]) != hashCalculator.hashOfTree(tree.children[1]))
			return false;

		TerminalERCUniform res = new TerminalERCUniform();
		res.children = new GPNode[0];
		DoubleERC erc = new DoubleERC();
		if(tree.toString().equals("/"))
			erc.value = 1;
		else if(tree.toString().equals("-"))
			erc.value = 0;
		else
			throw new RuntimeException("Not Sub and not Div?");

		res.setTerminal(erc);

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
//			treeParent = parent;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		return true;
	}

	private static boolean applyZeroDivided(GPNode tree)
	{
		if(!tree.toString().equals("/"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if( !isDoubleERC(tree.children[0], 0) )
			return false;

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = tree.children[0];
			tree.children[0].parent = parent;
			tree.children[0].argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = tree.children[0];
			tree.children[0].parent = parent;
			tree.children[0].argposition = tree.argposition;
//			treeParent = parent;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		tree.children[0] = null;
		return true;
	}

	private static boolean applyDivisionByUnity(GPNode tree)
	{
		if(!tree.toString().equals("/"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if(!isDoubleERC(tree.children[1], 1))
			return false;

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = tree.children[0];
			tree.children[0].parent = parent;
			tree.children[0].argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = tree.children[0];
			tree.children[0].parent = parent;
			tree.children[0].argposition = tree.argposition;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		tree.children[0] = null;
		return true;
	}

	private static boolean applyMultipliedByOne(GPNode tree)
	{
		if(!tree.toString().equals("*"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		GPNode ch1 = tree.children[0];
		GPNode ch2 = tree.children[1];

		if(!isDoubleERC(ch1, 1) && !isDoubleERC(ch2, 1))
			return false;

		GPNode res;
		if(isDoubleERC(ch1, 1))
			res = ch2;
		else
			res = ch1;

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		return true;
	}

	private static boolean applyMultipliedByZero(GPNode tree)
	{
		if(!tree.toString().equals("*"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		GPNode ch1 = tree.children[0];
		GPNode ch2 = tree.children[1];

		if(!isDoubleERC(ch1, 0) && !isDoubleERC(ch2, 0))
			return false;

		DoubleERC erc = new DoubleERC();
		erc.value = 0;
		TerminalERCUniform res = new TerminalERCUniform();
		res.children = new GPNode[0];
		res.setTerminal(erc);

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		return true;
	}

	private static boolean applyAddedByZero(GPNode tree)
	{
		if(!tree.toString().equals("+"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		GPNode ch1 = tree.children[0];
		GPNode ch2 = tree.children[1];

		if(!isDoubleERC(ch1, 0) && !isDoubleERC(ch2, 0))
			return false;

		GPNode res;
		if(isDoubleERC(ch1, 0))
			res = ch2;
		else
			res = ch1;

		Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = res;
			res.parent = parent;
			res.argposition = tree.argposition;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		return true;
	}

	private static boolean applySubtractedByZero(GPNode tree)
	{
		if(!tree.toString().equals("-"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		GPNode ch1 = tree.children[0];
		GPNode ch2 = tree.children[1];

		if(!isDoubleERC(ch2, 0))
			return false;

        Object oparent = tree.parent;
		tree.parent = null;

		if(oparent instanceof GPNode)
		{
			GPNode parent = (GPNode) oparent;
			parent.children[tree.argposition] = ch1;
			ch1.parent = parent;
			ch1.argposition = tree.argposition;
		}
		else if(oparent instanceof GPTree)
		{
			GPTree parent = (GPTree) oparent;
			parent.child = ch1;
			ch1.parent = parent;
			ch1.argposition = tree.argposition;
//			treeParent = parent;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		return true;
	}

	private boolean applyAllRules(GPNode tree)
	{

        if(tree.children == null || tree.children.length == 0)
            return false;

        boolean simplified = applyConstRule(tree);
		if(tree.parent == null)
			// The rule has discarded this tree so, no point in applying other rules to it anymore.
			return simplified;

		simplified |= applyAddedByZero(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applySubtractedByZero(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applyMultipliedByOne(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applyMultipliedByZero(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applySelfDivSubRule(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applyZeroDivided(tree);
		if(tree.parent == null)
			return simplified;

		simplified = applyMinMaxEqualRule(tree);
		if(tree.parent == null)
			return simplified;

		simplified |= applyDivisionByUnity(tree);
		if(tree.parent == null)
			return simplified;

		return simplified;
	}

	protected boolean simplify(GPNode tree)
	{
		if(tree.children == null || tree.children.length == 0)
			return false;

		boolean modified = false;
		boolean simplified;
		do
		{
			simplified = simplify(tree.children[0]);

			simplified |= simplify(tree.children[1]);

			simplified |= applyAllRules(tree);
			modified |= simplified;
			if(tree.parent == null)
				return modified;
		}
		while(simplified);

		return modified;
	}

	// TODO: This can be refactored into a helper class.
	private static void addNode(GPNode node, GPNode ch1, GPNode ch2)
	{
		node.children = new GPNode[2];

		node.children[0] = ch1;
		ch1.argposition = 0;
		ch1.parent = node;

		node.children[1] = ch2;
		ch2.argposition = 1;
		ch2.parent = node;
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException
	{
		TerminalERCUniform n1 = new TerminalERCUniform();
		ServeCost sc = new ServeCost();
		n1.children = new GPNode[0];
		n1.setTerminal(	sc);

		TerminalERCUniform n4 = new TerminalERCUniform();
		ServeCost sc2 = new ServeCost();
		n4.children = new GPNode[0];
		n4.setTerminal(sc2);

		TerminalERCUniform n2 = new TerminalERCUniform();
		RemainingCapacity rc = new RemainingCapacity();
		n2.children = new GPNode[0];
		n2.setTerminal(rc);

		TerminalERCUniform n3 = new TerminalERCUniform();
		RemainingCapacity rc2 = new RemainingCapacity();
		n3.children = new GPNode[0];
		n3.setTerminal(rc2);

		GPNode plus1 = new Add();
		addNode(plus1, n1, n2);
		GPNode plus2 = new Add();
		addNode(plus2, n3, n4);

		GPNode min = new Sub();
		addNode(min, plus1, plus2);

		GPTree tree = new GPTree();
		tree.child = min;
		min.parent = tree;
		min.argposition = 0;

		String paramFileNamePath = "src/tl/gphhucarp/target-wk.param";
		EvolutionState eState = loadECJ(paramFileNamePath);

        AlgebraicHashCalculator hasher = new AlgebraicHashCalculator(eState, 0, 3373);
		AlgebraicTreeSimplifier ts = new AlgebraicTreeSimplifier(hasher);

		System.out.println(tree.child.makeGraphvizTree());
		ts.simplify(min);
		System.out.println(tree.child.makeGraphvizTree());


//		String fileName = "/home/mazhar/MyPhD/SourceCodes/gpucarp/stats/source/population.gen.0.bin";
//		String fileName = "/vol/grid-solar/sgeusers/mazhar/val10D.vs10:gen_50/KnowledgeSource/38/TestedPopulation/population.gen.0.bin";
		String fileName = "/home/mazhar/grid/gdb1.vs5:gen_50/KnowledgeSource/1/population.gen.49.bin";
		Population p = PopulationUtils.loadPopulation(fileName);
		for(int i = 0; i < 500; i++)
		{
			GPIndividual ind = (GPIndividual)p.subpops[0].individuals[i];
			GPIndividual c = (GPIndividual) ind.clone();
			if(ts.simplify(ind.trees[0].child))
			{
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println("Before: " + c.trees[0].child.makeGraphvizTree() + "\n");
				System.out.println("After: " + ind.trees[0].child.makeGraphvizTree());
				System.out.println("----------------------------------------------------\n");
			}
		}

	}

	@Override
	protected boolean simplify(EvolutionState state, GPIndividual ind)
	{
		if(ind == null || ind.trees == null)
			throw new NullPointerException("GP individual or its trees cannot be null");

		boolean changed = false;
		for(GPTree tree : ind.trees)
			changed |= simplify(tree.child);

		return changed;
	}

}
