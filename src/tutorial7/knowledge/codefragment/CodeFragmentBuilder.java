package tutorial7.knowledge.codefragment;

import java.io.File;

import ec.EvolutionState;
import ec.gp.GPFunctionSet;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPNodeParent;
import ec.gp.GPProblem;
import ec.gp.GPType;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tutorial7.knowledge.*;
import tutorial7.knowledge.codefragment.fitted.*;
import tutorial7.knowledge.codefragment.simple.SimpleCodeFragmentKB;

public class CodeFragmentBuilder extends HalfBuilder
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_FILE = "knowledge-file";
	public static final String P_KNOWLEDGE_PROBABILITY = "knowledge-probability";

	private double knowledgeProbability = 0;

	// CodeFragmentKB knowledgeBase = new CodeFragmentKB();

	private KnowledgeExtractor extractor;


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
			state.output.fatal("Knowledge file does not exist: "
								+ fileName, knowledgeFileName);
		}

	    CodeFragmentKB knowledgeBase = new FittedCodeFragmentKB(state
	    		, (GPProblem)state.evaluator.p_problem, 20);
//		CodeFragmentKB knowledgeBase = new SimpleCodeFragmentKB();
		knowledgeBase.addFrom(kbFile, state);
		extractor = knowledgeBase.getKnowledgeExtractor();

		knowledgeProbability = state.parameters.getDouble(base.push(P_KNOWLEDGE_PROBABILITY), null);
	}

	protected GPNode growNode(final EvolutionState state, final int current, final int max,
							  final GPType type, final int thread, final GPNodeParent parent,
							  final int argposition, final GPFunctionSet set)
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
		if ((current+1 >= max) &&                            // Now pick if we're at max depth
				// this will freak out the static checkers
				(triedTerminals = true) &&                   // [first set triedTerminals]
				terminals.length != 0)                       // AND if there are available terminal
		{
			GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
			n.resetNode(state,thread);  // give ERCs a chance to randomize
			n.argposition = (byte)argposition;
			n.parent = parent;
			return n;
		}

		// else pick a random node
		else
		{
			if (triedTerminals) warnAboutNoTerminalWithType(type, false, state);        // we tried terminal and we're here because there were none!

//			GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
			GPNode n = null;
			double prob = state.random[thread].nextDouble();
			if(prob < knowledgeProbability && extractor.hasNext())
			{
				n = (GPNode) extractor.getNext().getItem();
				// if(n.depth() + current < max)
				{
					n.argposition = (byte)argposition;
					n.parent = parent;
					return n;
				}
				// else create a new node in the following line and do with it as usual.
			}

			n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());

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

	@Override
	protected GPNode fullNode(EvolutionState state, int current, int max, GPType type, int thread,
			GPNodeParent parent, int argposition, GPFunctionSet set)
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
			GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
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

			GPNode n = null;
			double prob = state.random[thread].nextDouble();
			if(prob < knowledgeProbability && extractor.hasNext())
			{
				n = (GPNode) extractor.getNext().getItem();
				// if(n.depth() + current < max)
				{
					n.argposition = (byte)argposition;
					n.parent = parent;
					return n;
				}
				// else create a new node in the following line and do with it as usual.
			}
			n = (GPNode)(nodesToPick[state.random[thread].nextInt(nodesToPick.length)].lightClone());
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
	public GPNode newRootedTree(EvolutionState state, GPType type, int thread, GPNodeParent parent,
			GPFunctionSet set, int argposition, int requestedSize)
	{
		return super.newRootedTree(state, type, thread, parent, set, argposition, requestedSize);
	}

}
