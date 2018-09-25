package tl.gp;

import ec.EvolutionState;
import ec.gp.GPFunctionSet;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPNodeParent;
import ec.gp.GPType;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import tl.knowledge.KnowledgeExtractor;

public abstract class CodeFragmentBuilder extends HalfBuilder
{
	private static final long serialVersionUID = 1L;

	public static final String P_KNOWLEDGE_PROBABILITY = "knowledge-probability";

	/**
	 * The default value for tournament size of the knowledge base. This value will be
	 * used if the <code>P_KNOWLEDGE_TOURNAMENT_SIZE</code> is not present.
	 */
	public static final int DEFAULT_KNOWLEDGE_TOURNAMENT_SIZE = 10;

	private double knowledgeProbability = 0;

	private KnowledgeExtractor extractor = null;

	@Override
	public void setup(EvolutionState state, Parameter base)
	{
		super.setup(state, base);

		knowledgeProbability = state.parameters.getDouble(base.push(P_KNOWLEDGE_PROBABILITY), null);
	}


	public abstract KnowledgeExtractor getKnowledgeExtractor();


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

		if(extractor == null)
			extractor = getKnowledgeExtractor();

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

			//GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
			GPNode n = null;
			double prob = state.random[thread].nextDouble();
			if(prob < Math.pow(knowledgeProbability, current + 1) && extractor.hasNext())
			{
				n = (GPNode) extractor.getNext().getItem();
				if(n.depth() + current < max)
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
		if(extractor == null)
			extractor = getKnowledgeExtractor();

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
			if(prob < Math.pow(knowledgeProbability, current + 1) && extractor.hasNext())
			{
				n = (GPNode) extractor.getNext().getItem();
				if(n.depth() + current < max)
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

}
