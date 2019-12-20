package tl.gp;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.Population;
import ec.gp.*;
import ec.gp.koza.HalfBuilder;
import ec.util.Parameter;
import org.apache.commons.lang3.ArrayUtils;
import tl.TLLogger;
import tl.knowledge.KnowledgeExtractionMethod;
import tl.knowledge.KnowledgeExtractor;
import tl.knowledge.codefragment.CodeFragmentKI;
import tl.knowledge.codefragment.simple.SimpleCodeFragmentKB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TournamentFullTreeBuilder extends HalfBuilder implements TLLogger<GPNode>
{
    private static final long serialVersionUID = 1L;

    public static final String P_KNOWLEDGE_FILE = "knowledge-file";

//	public static final String P_KNOWLEDGE_LOG_FILE_NAME = "knowledge-log-file";

    public static final String P_KNOWLEDGE_EXTRACTION_METHOD = "knowledge-extraction";

    public static final String P_TRANSFER_PERCENT = "transfer-percent";

    public static final String P_TOURNAMENT_SIZE = "tournament-size";

    /**
     * A boolean parameter that indicates if exact duplicates are allowed to be loaded and transferred or not. The default
     * value of this parameter is {@code true}.
     */
    public static final String P_ALLOW_DUPLICATES = "allow-duplicates";

//    protected static KnowledgeExtractor extractor = null;

    protected int knowledgeSuccessLogID;

    /**
     * Population loaded from knowledge file.
     */
    private Individual[] pop;


    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        super.setup(state, base);

        Parameter knowledgeFileName = base.push(P_KNOWLEDGE_FILE);
        String fileName = state.parameters.getString(knowledgeFileName, null);
        if(fileName == null)
        {
            state.output.fatal("Failed to load parameter", knowledgeFileName);
            return;
        }
        File kbFile = new File(fileName);
        if(!kbFile.exists())
        {
            state.output.fatal("Knowledge file does not exist: " + fileName, knowledgeFileName);
        }

        Parameter problemParam = new Parameter("pop.subpop.0.species.fitness");
        Fitness fitness = (Fitness) state.parameters.getInstanceForParameter(problemParam, null, Fitness.class);
        fitness.setup(state, problemParam);

        Parameter knowledgeExtraction = base.push(P_KNOWLEDGE_EXTRACTION_METHOD);
        String extraction = state.parameters.getString(knowledgeExtraction, null);
        KnowledgeExtractionMethod extractionMethod = KnowledgeExtractionMethod.parse(extraction);

        Parameter transferPercentParam = base.push(P_TRANSFER_PERCENT);
        double transferPercent = state.parameters.getDouble(transferPercentParam, null);
        if(transferPercent <= 0 || transferPercent > 1)
            state.output.fatal("Transfer percent must be in (0, 1]: " + transferPercent);

        boolean allowDup = state.parameters.getBoolean(base.push(P_ALLOW_DUPLICATES), null, true);

        Parameter tsize = base.push(P_TOURNAMENT_SIZE);
        int tournamentSize = state.parameters.getInt(tsize, null);



        state.output.warning("Allow duplicates: " + allowDup);


        try
        {
            int popsize = state.parameters.getInt(new Parameter("pop.subpop.0.size"), null);
            pop = PopulationUtils.loadPopulation(kbFile).subpops[0].individuals;
            int numToTransfer = (int) (popsize * transferPercent);
            for(; numToTransfer >= 0; numToTransfer--)
            {
                int selected = PopulationUtils.tournamentSelect(pop, state, 0, tournamentSize);
                Ss.add((GPIndividual) pop[selected]);
                pop = ArrayUtils.remove(pop, selected);
            }

//                    new ArrayList<>(Arr);

        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        knowledgeSuccessLogID = setupLogger(state, base);
//		}
//		 catch (IOException e) {
//			 e.printStackTrace();
//			state.output.fatal("Failed to create knowledge log file in CodeFragmentBuilder");
//			}
    }
    ArrayList<GPIndividual> Ss = new ArrayList<>();

    protected static int cfCounter = 0;


    public GPNode newRootedTree(final EvolutionState state, final GPType type, final int thread,
                                final GPNodeParent parent, final GPFunctionSet set, final int argposition,
                                final int requestedSize)
    {
        if(!Ss.isEmpty())
        {
//             Ss.get(0);
            GPIndividual cf = Ss.remove(0);
            GPNode root = GPIndividualUtils.stripRoots(cf).get(0);
            cfCounter++;
            log(state, knowledgeSuccessLogID, cfCounter + ": \t" + root.makeCTree(false, true, true) + "\n\n");
            root.parent = parent;
            return root;
        }
//        CodeFragmentKI cf = (CodeFragmentKI) extractor.getNext();
//        if(cf != null)
//        {
//            cfCounter++;
//            log(state, knowledgeSuccessLogID, cfCounter + ": \t" + cf.toString() + "\n\n");
//            GPNode node = cf.getItem();
//            node.parent = parent;
//            return node;
//        }
//        else
//            log(state, null, knowledgeSuccessLogID);

        if (state.random[thread].nextDouble() < pickGrowProbability)
            return growNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
        else
            return fullNode(state,0,state.random[thread].nextInt(maxDepth-minDepth+1) + minDepth,type,thread,parent,argposition,set);
    }
//
//	private void log(EvolutionState state, CodeFragmentKI it, int logID)
//	{
//		state.output.println(cfCounter + ": \t" + (it == null ? "null" : it.toString()), logID);
//		state.output.flush();
//		state.output.println("", logID);
//	}
}
