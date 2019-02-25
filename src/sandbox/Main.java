package sandbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Random;

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

public class Main {

//	public static long[] xgcd(long a, long b){
//		long[] retvals={0,0,0};
//		long aa[]={1,0}, bb[]={0,1}, q=0;
//		while(true) {
//			q = a / b; a = a % b;
//			aa[0] = aa[0] - q*aa[1];  bb[0] = bb[0] - q*bb[1];
//			if (a == 0) {
//				retvals[0] = b; retvals[1] = aa[1]; retvals[2] = bb[1];
//				return retvals;
//			};
//			q = b / a; b = b % a;
//			aa[1] = aa[1] - q*aa[0];  bb[1] = bb[1] - q*bb[0];
//			if (b == 0) {
//				retvals[0] = a; retvals[1] = aa[0]; retvals[2] = bb[0];
//				return retvals;
//			};
//		}
//	}

	private static EvolutionState state = null;

	static void loadECJ(String paramFileNamePath, String... ecjParams)
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

		state = Evolve.initialize(parameters, 0);

		Parameter p;

		// setup the evaluator, essentially the test evaluation model
		p = new Parameter(EvolutionState.P_EVALUATOR);
		state.evaluator = (Evaluator)
				(parameters.getInstanceForParameter(p, null, Evaluator.class));
		state.evaluator.setup(state, p);

		p = new Parameter("eval.problem.eval-model.instances.0.samples");
		int samples = state.parameters.getInt(p, null);
		if(samples < 100)
			state.output.fatal("Sample size is too small: " + samples);
		else
			state.output.warning("Sample size in AnalyzeTerminals: " + samples);
	}

	private static int mod(int a, int b)
	{
		int ret = a % b;
		while(ret < 0)
			ret += b;

		return ret;
	}

	private static int eea(int a, int b, int p)
	{
		if(b == 0)
			return 1; // TODO: Are you sure?
		int r = 1;

		int q0 = a / b;
		r = a - q0 * b;
		int x0 = 0;
		if(r == 0)
			return x0;

		a = b;
		b = r;
		int q1 = a / b;
		r = a - q1 * b;
		int x1 = 1;
		if(r == 0)
			return x1;

		while(r != 0)
		{
			a = b;
			b = r;

			int q = a / b;
			r = a - q * b;

			int x2 = mod((x0 - x1*q0), p);
			x0 = x1;
			x1 = x2;
			q0 = q1;
			q1 = q;
		}

		int x2 = mod((x0 - x1*q0), p);
		return x2;
	}

	private static ArrayList<Integer> seenNumbers = new ArrayList<>();
	private static int nextRand()
	{
		Random rng = new Random();
		int rnd = rng.nextInt(prime); //state.random[0].nextInt(prime);
		while(seenNumbers.contains(rnd) == true)
			rnd = rng.nextInt(prime); //state.random[0].nextInt(prime);

		seenNumbers.add(rnd);
		return rnd;
	}

	private static int SCHash = nextRand();
	private static int CFDHash = nextRand();
	private static int CFHHash = nextRand();
	private static int CTDHash = nextRand();
	private static int CRHash = nextRand();
	private static int DCHash = nextRand();
	private static int DEMHash = nextRand();
	private static int RQHash = nextRand();
	private static int FULLHash = nextRand();
	private static int FRTHash = nextRand();
	private static int FUTHash = nextRand();
	private static int CFR1Hash = nextRand();
	private static int CTT1Hash = nextRand();
	private static int DEM1Hash = nextRand();

	private static int hashOf(TerminalERCUniform t)
	{
		String name = t.getTerminal().name();
		switch(name)
		{
		case "SC":
			return SCHash;
		case "CFD":
			return CFDHash;
		case "CFH":
			return CFHHash;
		case "CTD":
			return CTDHash;
		case "CR":
			return CRHash;
		case "DC":
			return DCHash;
		case "DEM":
			return DEMHash;
		case "RQ":
			return RQHash;
		case "FULL":
			return FULLHash;
		case "FRT":
			return FRTHash;
		case "FUT":
			return FUTHash;
		case "CFR1":
			return CFR1Hash;
		case "CTT1":
			return CTT1Hash;
		case "DEM1":
			return DEM1Hash;
		case "ERC":
			double value = ((DoubleERC)t.getTerminal()).value;
			return hashOf(value);
		default:
			throw new RuntimeException("Received an unknown terminal to hash: " + name);
		}
	}

	private static final int prime = 101;

	private static int hashOf(double value)
	{
		value *= 10;
		int cd = (int) value;
		int retval = mod(cd * eea(prime, 10, prime), prime);
		return retval;
	}

	private static int hashOfTree(GPNode tree)
	{
		if(tree.children == null || tree.children.length == 0)
			return hashOf((TerminalERCUniform) tree);
		int lch = hashOfTree(tree.children[0]); // left child hash
		int rch = hashOfTree(tree.children[1]);
		if(tree.toString().equals("+"))
			return mod(lch + rch, prime);
		if(tree.toString().equals("-"))
			return mod(lch - rch, prime);
		if(tree.toString().equals("*"))
			return mod(lch * rch, prime);
		if(tree.toString().equals("/"))
			return hashOf((double)lch / (double)rch);
		if(tree.toString().equals("min"))
		{
			return hashOf(eea( mod(lch - rch, prime), lch, prime) + rch);
			// return Math.min(lch, rch);
		}
		if(tree.toString().equals("max"))
		{
			return hashOf(eea(mod(lch - rch, prime), rch, prime) + lch);
			//return Math.max(lch, rch);
		}
		throw new RuntimeException("Received an unknown ternimal type: " + tree.toString());
	}


	private static boolean isDoubleERC(GPNode node, double value)
	{
		if( (node instanceof TerminalERCUniform)
				&& ((TerminalERCUniform)node).getTerminal().name().equals("ERC")
				&& ((DoubleERC)((TerminalERCUniform)node).getTerminal()).value == value)
			return true;
		return false;
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
		if(operation.equals("+"))
		{
			erc.value = value1 + value2;
		}
		else if(operation.equals("-"))
		{
			erc.value = value1 - value2;
		}
		else if(operation.equals("*"))
		{
			erc.value = value1 * value2;
		}
		else if(operation.equals("/"))
		{
			erc.value = value1 / (value2 == 0 ? 1 : value2);
		}
		else if(operation.equals("min"))
		{
			erc.value = Math.min(value1, value2);
		}
		else if(operation.equals("max"))
		{
			erc.value = Math.max(value1, value2);
		}
		else
			throw new RuntimeException("Unknown operation to simplify.");

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

	private static boolean applyMinMaxEqualRule(GPNode tree)
	{
		if(!tree.toString().equals("min") && !tree.toString().equals("max"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if(hashOfTree(tree.children[0]) != hashOfTree(tree.children[1]))
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
//			treeParent = parent;
		}
		else
			throw new RuntimeException("Unknown parent type: " + oparent);

		return true;
	}

	private static boolean applySelfDivSubRule(GPNode tree)
	{
		if(!tree.toString().equals("/") && !tree.toString().equals("-"))
			return false;
		if(tree.children == null || tree.children.length == 0)
			throw new RuntimeException("Received a division operator without any operands");

		if(hashOfTree(tree.children[0]) != hashOfTree(tree.children[1]))
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
//			treeParent = parent;
		}
		else
			throw new RuntimeException("Parent type is not a tree or a node: " + oparent);

		tree.children[0] = null;
		return true;
	}

//	static GPTree treeParent = null;

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

		GPNode res = null;
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
//			treeParent = parent;
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
//			treeParent = parent;
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

		GPNode res = null;
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
//			treeParent = parent;
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

		GPNode res = ch1;

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

	//	static boolean selfSubtracted(GPNode tree)
	//	{
	//		if(!tree.toString().equals("-"))
	//			return false;
	//		if(tree.children == null || tree.children.length == 0)
	//			throw new RuntimeException("Received a division operator without any operands");
	//
	//		GPNode ch1 = tree.children[0];
	//		GPNode ch2 = tree.children[1];
	//
	//		if(!isDoubleERC(ch1, 0) && !isDoubleERC(ch2, 0))
	//			return false;
	//
	//		GPNode res = null;
	//		if(isDoubleERC(ch1, 0))
	//			res = ch2;
	//		else
	//			res = ch1;
	//
	//		Object oparent = tree.parent;
	//		tree.parent = null;
	//
	//		if(oparent instanceof GPNode)
	//		{
	//			GPNode parent = (GPNode) oparent;
	//			parent.children[tree.argposition] = res;
	//			res.parent = parent;
	//			res.argposition = tree.argposition;
	//		}
	//		else if(oparent instanceof GPTree)
	//		{
	//			GPTree parent = (GPTree) oparent;
	//			parent.child = res;
	//			res.parent = parent;
	//			res.argposition = tree.argposition;
	//		}
	//
	//		return true;
	//	}


	private static boolean applyAllRules(GPNode tree)
	{
		boolean simplified = false;

		if(tree.children == null || tree.children.length == 0)
			return simplified;

		simplified = false;

		simplified |= applyConstRule(tree);
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

	public static boolean simplify(GPNode tree)
	{
		if(tree.children == null || tree.children.length == 0)
			return false;

		boolean modified = false;
		boolean simplified;
		do
		{
			simplified = false;
			simplified |= simplify(tree.children[0]);

			simplified |= simplify(tree.children[1]);

			simplified |= applyAllRules(tree);
			modified |= simplified;
			if(tree.parent == null)
				return modified;
		}
		while(simplified == true);

		return modified;
	}

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

	public static void main(String[] args) throws InvalidObjectException, FileNotFoundException, ClassNotFoundException, IOException
	{
		String fileName2 = "population.gen.21.bin";
		int index = fileName2.indexOf("gen.") + 4;
		String num = fileName2.substring(index, fileName2.indexOf(".bin"));


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

		System.out.println(tree.child.makeGraphvizTree());
		simplify(min);
		System.out.println(tree.child.makeGraphvizTree());


		String fileName = "/home/mazhar/MyPhD/SourceCodes/gpucarp/stats/source/population.gen.0.bin";
		Population p = PopulationUtils.loadPopulation(fileName);
		for(int i = 0; i < 500; i++)
		{
			GPIndividual ind = (GPIndividual)p.subpops[0].individuals[i];
			GPIndividual c = (GPIndividual) ind.clone();
			if(simplify(ind.trees[0].child))
			{
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println("Before: " + c.trees[0].child.makeGraphvizTree() + "\n");
				System.out.println("After: " + ind.trees[0].child.makeGraphvizTree());
				System.out.println("----------------------------------------------------\n");
			}
		}

	};

}
