package sandbox;

import ec.EvolutionState;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.koza.GrowBuilder;
import ec.gp.koza.KozaNodeSelector;
import ec.util.Parameter;
import gphhucarp.gp.terminal.feature.Fullness;
import gphhucarp.gp.terminal.feature.RemainingCapacity;
import gphhucarp.gp.terminal.feature.ServeCost;
import gputils.function.Add;
import gputils.function.Mul;
import gputils.terminal.TerminalERCUniform;
import tl.gp.GPIndividualUtils;
import tl.gp.Mutator;
import tl.gp.hash.AlgebraicHashCalculator;
import tl.gp.hash.VectorialAlgebraicHashCalculator;

import static tl.ecj.ECJUtils.loadECJ;
import static tl.gp.GPIndividualUtils.addChildrenTo;

public class Main {

//	private static EvolutionState state = null;

	static TerminalERCUniform createTerminal()
	{
		TerminalERCUniform scn1 = new TerminalERCUniform();
		ServeCost sc = new ServeCost();
		scn1.children = new GPNode[0];
		scn1.setTerminal(sc);

		return scn1;
	}

	public static void main(String[] args)
	{
//		String paramFileNamePath = "./bin/tl/gphhucarp/target-wk.param";
//		EvolutionState eState = loadECJ(paramFileNamePath);
//
//		GPIndividual ind = GPIndividualUtils.sampleInd();
//		System.out.println(ind.trees[0].child.makeGraphvizTree() + "\n");
//
//		KozaNodeSelector nodeSelector = new KozaNodeSelector(0, 0.9, 0.1);
//		GrowBuilder builder = new GrowBuilder();
//		builder.setup(eState, new Parameter("gp.koza.grow"));
//		Mutator mutator = new Mutator(eState, nodeSelector, builder, 3, 8, -1, false);
//		GPIndividual mutated = mutator.mutate(0, ind, eState, 0, 0);
//
//		System.out.println(mutated.trees[0].child.makeGraphvizTree());

//		// Create FULL * (SC + RQ)
//		TerminalERCUniform scn1 = new TerminalERCUniform();
//		ServeCost sc = new ServeCost();
//		scn1.children = new GPNode[0];
//		scn1.setTerminal(sc);
//
//		TerminalERCUniform rqn1 = new TerminalERCUniform();
//		RemainingCapacity rc1 = new RemainingCapacity();
//		rqn1.children = new GPNode[0];
//		rqn1.setTerminal(rc1);
//
//		TerminalERCUniform fuln1 = new TerminalERCUniform();
//		Fullness ful1 = new Fullness();
//		fuln1.children = new GPNode[0];
//		fuln1.setTerminal(ful1);
//
//		GPNode plus1 = new Add();
//		addChildrenTo(plus1, false, scn1, rqn1);
//		GPNode mul1 = new Mul();
//		addChildrenTo(mul1, false, fuln1, plus1);
//
//		VectorialAlgebraicHashCalculator hs = new VectorialAlgebraicHashCalculator(eState, 0, 3, 1000);
//		System.out.println(mul1.makeCTree(true, true, true));
//		System.out.println(hs.hashOfTree(mul1));
//
//
//		// Create FULL * SC + FULL * RQ
//		TerminalERCUniform fuln2 = new TerminalERCUniform();
//		Fullness ful2 = new Fullness();
//		fuln2.children = new GPNode[0];
//		fuln2.setTerminal(ful2);
//
//		TerminalERCUniform fuln3 = new TerminalERCUniform();
//		Fullness ful3 = new Fullness();
//		fuln3.children = new GPNode[0];
//		fuln3.setTerminal(ful3);
//
//		TerminalERCUniform rqn2 = new TerminalERCUniform();
//		RemainingCapacity rc2 = new RemainingCapacity();
//		rqn2.children = new GPNode[0];
//		rqn2.setTerminal(rc2);
//
//		TerminalERCUniform scn2 = new TerminalERCUniform();
//		ServeCost sc2 = new ServeCost();
//		scn2.children = new GPNode[0];
//		scn2.setTerminal(sc2);
//
//		GPNode mul2 = new Mul();
//		GPNode mul3 = new Mul();
//		GPNode plus2 = new Add();
//		addChildrenTo(mul2, false, fuln2, scn2);
//		addChildrenTo(mul3, false, fuln3, rqn2);
//		addChildrenTo(plus2, false, mul2, mul3);
//
//		System.out.println();
//		System.out.println(plus2.makeCTree(true, true, true));
//		System.out.println(hs.hashOfTree(plus2));


	}
}
