package tl.gp;

import ec.EvolutionState;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import javafx.util.Pair;
import tl.TLLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;

/**
 * @author mazhar
 */
public class ContribSubtreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    private static final long serialVersionUID = 1L;

    /**
     * The percentage of initial population that is created from extracted knowledge. The value must
     * be in range (0, 1].
     */
    public static final String P_TRANSFER_PERCENT = "transfer-percent";


    /**
     * The path to the file that contains subtree information.
     */
    public static final String P_KNOWLEDGE_FILE = "knowledge-file";


    private int knowledgeSuccessLogID;

    /**
     * The value for the {@code P_TRANSFER_PERCENT} parameter which is the percentage of initial
     * population that is transfered from extracted knowledge. The value must be in range (0, 1].
     */
    private double transferPercent;

    private static int cfCounter = 0;
    private Iterator<TLGPIndividual> iter;

    @Override
    public void setup(EvolutionState state, Parameter base) {
        super.setup(state, base);

        knowledgeSuccessLogID = setupLogger(state, base);

        Parameter p = base.push(P_TRANSFER_PERCENT);
        transferPercent = state.parameters.getDouble(p, null);

        String knowFile = state.parameters.getString(base.push(P_KNOWLEDGE_FILE), null);
        if (knowFile == null)
            state.output.fatal("Knowledge file name cannot be null");
        loadSubtrees(state, knowFile);


//		FrequentCodeFragmentKB knowledgeBase =
//				new FrequentCodeFragmentKB(state, extractPercent, minDepth, maxDepth);
//		knowledgeBase.extractFrom(kbDirectory, ".bin", extractionMethod);
//		extractor = knowledgeBase.getKnowledgeExtractor();
//		state.output.warning("DepthedFrequentSimpleCodeFragmentBuilder loaded. Transfer percent: "
//							 + transferPercent + ", extraction method: " + extractionMethod
//							 + ", min depth: " + minDepth + ", max depth: " + maxDepth);
    }


    @SuppressWarnings("unchecked")
    private void loadSubtrees(EvolutionState state, String knowFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(knowFile))) {
            double minFit = ois.readDouble(); // read minFit
            double maxFit = ois.readDouble(); // read maxFit
            HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> subtrees
                    = (HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>>) ois.readObject();

            log(state, knowledgeSuccessLogID, "Loaded knowledge base. MinFit: " + minFit + ", maxFit: " + maxFit + ". "
                                                + "Database size: " + subtrees.size());

            calculateWeights(subtrees, minFit, maxFit);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

//	private ArrayList<TLGPIndividual> kItems = new ArrayList<>();
//	private double[] kWeights = null;

    private void calculateWeights(HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> subtree,
                                  double minFit, double maxFit) {
        /*
         * This structure contains the knowledge transferred from source domain. The key is the
         * transferred subtree and the value is the weight of the subtree based on its contribution in
         * the source domain.
         */
        HashMap<TLGPIndividual, Double> knowledgeBase = new HashMap<>();

        double gmax = 1.0 / (1 + minFit);
        double gmin = 1.0 / (1 + maxFit);

        for (GPIndividual ind : subtree.keySet()) {
            ArrayList<Pair<GPNode, Double>> subs = subtree.get(ind);
            for (Pair<GPNode, Double> pair : subs)
            {
                TLGPIndividual i = GPIndividualUtils.asGPIndividual(pair.getKey());
                double contrib = pair.getValue();
                double nfit = 1f / (ind.fitness.fitness() + 1);
                nfit = Math.max(0, (nfit - gmin) / (gmax - gmin));
                // contrib is (oldfit - newfit) so a positive value means negative impact on fitness
                // That is why -contrib is used.
                if (-contrib > 0.001)
                {
                    if (knowledgeBase.containsKey(i))
                    {
//						int index = kItems.indexOf(i);
                        double weight = knowledgeBase.get(i);
                        weight += nfit * (-contrib);
                        knowledgeBase.put(i, weight);
                    }
                    else
                    {
                        knowledgeBase.put(i, nfit);
                    }
                }
            }
        }

        skb = sortByValue(knowledgeBase); // sorted knowledge base
        iter = skb.keySet().iterator();
    }

    private Map<TLGPIndividual, Double> skb =null; // sorted knowledge base

//    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map)
//    {
//        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
//
//        Comparator<Map.Entry<K, V>> cmp = (Comparator<Map.Entry<K, V>> & Serializable) (c1, c2) -> c2.getValue().compareTo(c1.getValue());
//
//        list.sort(cmp);
//
//        Map<K, V> result = new LinkedHashMap<>();
//        for (Map.Entry<K, V> entry : list) {
//            result.put(entry.getKey(), entry.getValue());
//        }
//
//        return result;
//    }

    private static HashMap<TLGPIndividual, Double> sortByValue(HashMap<TLGPIndividual, Double> map)
    {
        List<Map.Entry<TLGPIndividual, Double>> list = new ArrayList<>(map.entrySet());

        Comparator<Map.Entry<TLGPIndividual, Double>> cmp =
                (Comparator<Map.Entry<TLGPIndividual, Double>> & Serializable) (c1, c2) -> c2.getValue().compareTo(c1.getValue());

        list.sort(cmp);

        HashMap<TLGPIndividual, Double> result = new LinkedHashMap<>();
        for (Map.Entry<TLGPIndividual, Double> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

//        result.forEach((k, v) -> {System.out.println(k.trees[0].child);});

        return result;
    }


    @Override
    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
                                final GPNodeParent parent, final GPFunctionSet set, final int argposition,
                                final int requestedSize)
    {
        int popSize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
        int numToTransfer = (int) Math.round(popSize * transferPercent);
        if (numToTransfer >= 0 && cfCounter < numToTransfer && iter.hasNext()) {
//			CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
            // cloned because otherwise, the stripRoot method below will corrupt knowledge base
            TLGPIndividual cf = iter.next();

            if (cf != null) {
                cfCounter++;
                double contrib = skb.get(cf);
                GPNode node = GPIndividualUtils.stripRoots((GPIndividual) cf.clone()).get(0);
                log(state, knowledgeSuccessLogID, node.makeCTree(true, true, true)+ " contrib: " + contrib + "\n\n");
//                log(state, node, knowledgeSuccessLogID);
                node.parent = parent;
                node.argposition = (byte) argposition;
                return node;
            } else
                log(state, null, cfCounter, knowledgeSuccessLogID);
        }
        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth,
                    type, thread, parent, argposition, set);
        else
            return fullNode(state, 0, state.random[thread].nextInt(maxDepth - minDepth + 1) + minDepth,
                    type, thread, parent, argposition, set);
    }

}
