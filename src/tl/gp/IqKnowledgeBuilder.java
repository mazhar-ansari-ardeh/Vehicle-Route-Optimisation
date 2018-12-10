package tl.gp;

import java.io.File;
import java.util.ArrayList;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;
import tl.TLLogger;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.SinglePassKnowledgeExtractor;
import tl.knowledge.codefragment.simple.IqKB;



public class IqKnowledgeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
	private static final long serialVersionUID = 1L;

	// 	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_KNOWLEDGE_FOLDER = "knowledge-folder";

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";

	public static final String P_CF_MAX_DEPTH = "cf-depth";

	private static SinglePassKnowledgeExtractor<GPNode> extractor = null;

	private int knowledgeSuccessLogID;

	ArrayList<CFTerminal> codeFragmentNodes = new ArrayList<>();

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		Parameter knowledgeFileName = base.push(P_KNOWLEDGE_FILE);
		String fileName = state.parameters.getString(knowledgeFileName, null);
		if(fileName == null)
		{
			state.output.fatal("Failed to load parameter", knowledgeFileName);
		}
		File kbFile = new File(fileName);
		if(kbFile.exists() == false)
		{
			state.output.fatal("Knowledge file does not exist: " + fileName, knowledgeFileName);
		}

		int cfDepth = state.parameters.getIntWithDefault(base.push(P_CF_MAX_DEPTH), null, 2);

		IqKB knowledgeBase = new IqKB(state, 0, cfDepth);
		knowledgeBase.extractFrom(kbFile, KnowledgeExtractionMethod.AllSubtrees);
		extractor = knowledgeBase.getKnowledgeExtractor();
		while(extractor.hasNext())
		{
			CodeFragmentKI ki = (CodeFragmentKI) extractor.getNext();
//			GPIndividual ind = ki.getAsIndividual();
//			((SimpleProblemForm)state.evaluator.p_problem).evaluate(state, ind, 0, 0);
			codeFragmentNodes.add(new CFTerminal(ki, ((SimpleProblemForm)state.evaluator.p_problem)));
		}

		state.output.warning("IqKnowledgeBuilder loaded. CF max depth: " + cfDepth);

		knowledgeSuccessLogID = setupLogger(state, base);
	}

	static int cfCounter = 0;

	/** A private recursive method which builds a FULL-style tree for newRootedTree(...) */
	protected GPNode fullNode(final EvolutionState state,
			final int current,
			final int max,
			final GPType type,
			final int thread,
			final GPNodeParent parent,
			final int argposition,
			final GPFunctionSet set)
	{
		// fullNode can mess up if there are no available terminal for a given type.  If this occurs,
		// and we find ourselves unable to pick a terminal when we want to do so, we will issue a warning,
		// and pick a nonterminal, violating the "FULL" contract.  This can lead to pathological situations
		// where the system will continue to go on and on unable to stop because it can't pick a terminal,
		// resulting in running out of memory or some such.  But there are cases where we'd want to let
		// this work itself out.
		boolean triedTerminals = false;   // did we try -- and fail -- to fetch a terminal?

		int t = type.type;
		GPNode[] terminals = set.terminals[t];
		GPNode[] nonterminals = set.nonterminals[t];
		GPNode[] nodes = set.nodes[t];

		if (nodes.length == 0)
			errorAboutNoNodeWithType(type, state);   // total failure

		// pick a terminal when we're at max depth or if there are NO nonterminals
		if ((  current+1 >= max ||                                                      // Now pick if we're at max depth
				warnAboutNonterminal(nonterminals.length==0, type, false, state)) &&     // OR if there are NO nonterminals!
				// this will freak out the static checkers
				(triedTerminals = true) &&                                                  // [first set triedTerminals]
				terminals.length != 0)                                                      // AND if there are available terminal
		{
			GPNode n = null;
			if(state.random[thread].nextDouble() <= 0.5)
			{
				n = codeFragmentNodes.get(state.random[thread].nextInt(codeFragmentNodes.size()));
				log(state, new CodeFragmentKI(n), ++cfCounter, knowledgeSuccessLogID);
			}
			else
			{
				n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
			}
			n.resetNode(state,thread);  // give ERCs a chance to randomize
			n.argposition = (byte)argposition;
			n.parent = parent;
			return n;
		}

		// else force a nonterminal unless we have no choice
		else
		{
			if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!

			GPNode[] nodesToPick = set.nonterminals[type.type];
			if (nodesToPick==null || nodesToPick.length ==0)                            // no nonterminals, hope the guy knows what he's doing!
				nodesToPick = set.terminals[type.type];                                 // this can only happen with the warning about nonterminals above

			GPNode n = (GPNode)(nodesToPick[state.random[thread].nextInt(nodesToPick.length)].lightClone());
			n.resetNode(state,thread);  // give ERCs a chance to randomize
			n.argposition = (byte)argposition;
			n.parent = parent;

			// Populate the node...
			GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;
			for(int x=0;x<childtypes.length;x++)
				n.children[x] = fullNode(state,current+1,max,childtypes[x],thread,n,x,set);

			return n;
		}
	}

	@Override
	protected GPNode growNode(final EvolutionState state,
			final int current,
			final int max,
			final GPType type,
			final int thread,
			final GPNodeParent parent,
			final int argposition,
			final GPFunctionSet set)
	{
		// growNode can mess up if there are no available terminal for a given type.  If this occurs,
		// and we find ourselves unable to pick a terminal when we want to do so, we will issue a warning,
		// and pick a nonterminal, violating the maximum-depth contract.  This can lead to pathological situations
		// where the system will continue to go on and on unable to stop because it can't pick a terminal,
		// resulting in running out of memory or some such.  But there are cases where we'd want to let
		// this work itself out.
		boolean triedTerminals = false;

		int t = type.type;
		GPNode[] terminals = set.terminals[t];
		// GPNode[] nonterminals = set.nonterminals[t];
		GPNode[] nodes = set.nodes[t];

		if (nodes.length == 0)
			errorAboutNoNodeWithType(type, state);   // total failure

		// pick a terminal when we're at max depth or if there are NO nonterminals
		if ((current+1 >= max) &&                                                       // Now pick if we're at max depth
				// this will freak out the static checkers
				(triedTerminals = true) &&                                                  // [first set triedTerminals]
				terminals.length != 0)                                                      // AND if there are available terminal
		{

			GPNode n = null;
			if(state.random[thread].nextDouble() <= 0.5)
			{
				n = codeFragmentNodes.get(state.random[thread].nextInt(codeFragmentNodes.size()));
				log(state, new CodeFragmentKI(n), ++cfCounter, knowledgeSuccessLogID);
			}
			else
			{
				n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
			}
			n.resetNode(state,thread);  // give ERCs a chance to randomize
			n.argposition = (byte)argposition;
			n.parent = parent;
			return n;
		}

		// else pick a random node
		else
		{
			if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!

			GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
			n.resetNode(state,thread);  // give ERCs a chance to randomize
			n.argposition = (byte)argposition;
			n.parent = parent;

			// Populate the node...
			GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;
			for(int x=0;x<childtypes.length;x++)
				n.children[x] = growNode(state,current+1,max,childtypes[x],thread,n,x,set);

			return n;
		}
	}

}
