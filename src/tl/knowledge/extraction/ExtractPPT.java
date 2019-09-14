package tl.knowledge.extraction;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import tl.TLLogger;
import tl.ecj.ECJUtils;
import tl.gp.PopulationUtils;
import tl.gphhucarp.UCARPUtils;
import tl.knowledge.ppt.pipe.FrequencyLearner;
import tl.knowledge.ppt.pipe.PPTree;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class Extractor
{
    EvolutionState state = null;

//    /**
//     * The percent of the individuals in the source domain to consider to analyzing. The value of
//     * this parameter must be in [0, 1].
//     * If this value is less than 1, the individuals in the population will be sorted according to
//     * their fitness and then will be selected.
//     */
//    private double percent;

    public final String P_BASE = "extract-ppt";

//    public final String P_PERCENT = "percent";

    /**
     * Total number of generations on the source domain. This parameter is counted from 1.
     */
    public final String P_NUM_GENERATIONS = "num-generations";
    private int numGenerations = -1;

    /**
     * When the knowledge source is a directory, this parameter specifies from which generation on
     * the populations should be read. This parameter is inclusive.
     */
    public final String P_GENERATION_FROM = "from-generation";
    private int fromGeneration = -1;


    /**
     * When the knowledge source is a directory, this parameter specifies the generation until which
     * the populations should be read. This parameter is inclusive.
     */
    public final String P_GENERATION_TO = "to-generation";
    private int toGeneration = -1;

//    /**
//     * Minimum allowed size of code fragments to use, inclusive.
//     */
//    public final String P_MIN_ST_DEPTH = "min-st-depth";
//    private int minDepth;

//    /**
//     * Maximum allowed size of code fragments to use, inclusive.
//     */
//    public final String P_MAX_ST_DEPTH = "max-st-depth";
//    private int maxDepth;

    /**
     * This program implements a simple niching algorithm and this parameter indicates its radius.
     */
    public final String P_FITNESS_NICHE_RADIUS = "fitness-niche-radius";
    private double fitnessNicheRadius;


    /**
     * Number of individuals that were analyzed.
     */
//    int numAnalyzed = 0;

    private TLLogger<GPNode> logger;

    private int logID;

    /**
     * The learning rate for learning.
     */
    private double lr;
    public static final String P_LEARNING_RATE = "lr";

    /**
     * The size of the set that is sampled from the population set to learn from.
     */
    private int sampleSize;
    public static final String P_SAMPLE_SIZE = "sample-size";

    /**
     * The tournament size to sample individuals from the population to learn.
     */
    private int tournamentSize;
    public final static String P_TOURNAMENT_SIZE = "tournament-size";

    /**
     * The learner that will be used to learn the PPT from the population.
     */
    private FrequencyLearner learner;

    private PPTree tree;

//    private HashMap<GPIndividual, ArrayList<Pair<GPNode, Double>>> book = new HashMap<>();

    private void loadECJ(String paramFileNamePath, String... ecjParams)
    {
        state = ECJUtils.loadECJ(paramFileNamePath, ecjParams);

        Parameter base = new Parameter(P_BASE);
//        Parameter p = base.push(P_PERCENT);
//        percent = state.parameters.getDouble(p, null);
//        if(percent < 0 || percent > 1)
//            state.output.fatal("K percent is not a valid value for AnalyzeSubtrees: " + percent);
//        state.output.warning("Percent is: " + percent);

        fromGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_FROM), null, -1);
        state.output.warning("From generation: " + fromGeneration);
        toGeneration = state.parameters.getIntWithDefault(base.push(P_GENERATION_TO), null, -1);
        state.output.warning("To generation: " + toGeneration);
        fitnessNicheRadius = state.parameters.getDouble(base.push(P_FITNESS_NICHE_RADIUS), null);
        state.output.warning("Niche radius: " + fitnessNicheRadius);

//        p = base.push(P_PPT_LEARN);
//        String extraction = state.parameters.getString(p, null);
//        if(extraction == null)
//            state.output.fatal("Extraction method cannot be null");
//        extractionMethod = KnowledgeExtractionMethod.parse(extraction);
//        state.output.warning("Extraction method: " + extractionMethod);

//        p = base.push(P_MIN_ST_DEPTH);
//        minDepth = state.parameters.getInt(p, null);
//        state.output.warning("Min depth: " + minDepth);
//        p = base.push(P_MAX_ST_DEPTH);
//        maxDepth = state.parameters.getInt(p, null);
//        state.output.warning("Max depth: " + maxDepth);

        Parameter p = new Parameter("eval.problem.eval-model.instances.0.samples");
        int samples = state.parameters.getInt(p, null);
        if(samples < 100)
            state.output.fatal("Sample size is too small: " + samples);
        else
            state.output.warning("Sample size in ExtractPPT: " + samples);

        logger = new TLLogger<GPNode>()
        {
        };

        p = base.push(P_LEARNING_RATE);
        this.lr = state.parameters.getDouble(p, null);
        if(this.lr <= 0 || this.lr > 1)
            state.output.fatal("The value of the learning rate is invalid:" + this.lr);
        else
            state.output.warning("Learning rate: " + this.lr);

        p = base.push(P_SAMPLE_SIZE);
        this.sampleSize = state.parameters.getInt(p, null);
        if(sampleSize <= 0)
            state.output.fatal("Sample size must be a positive value: " + sampleSize);
        else
            state.output.warning("Sample size: " + this.sampleSize);

        p = base.push(P_TOURNAMENT_SIZE);
        this.tournamentSize = state.parameters.getInt(p, null);
        if(tournamentSize <= 0 || tournamentSize > this.sampleSize)
            state.output.fatal("Tournament size must be positive and smaller than sample size: " + tournamentSize);
        else
            state.output.warning("Tournament size: " + this.tournamentSize);

        logID = logger.setupLogger(state, base);

        p = base.push(P_NUM_GENERATIONS);
        numGenerations = state.parameters.getInt(p, null);
        state.output.warning("Number of generations on source domain: " + numGenerations);
        state.output.warning("ExtractPPT loaded.");
    }

    private ArrayList<Population> readPopulation(String inputFileNamePath) throws IOException, ClassNotFoundException
    {
        //	String inputFileNamePath = "/vol/grid-solar/sgeusers/mazhar/gdb2-v7-to8/1/stats/gdb2-v7-writeknow/population.gen.49.bin";
//        String inputFileNamePath = args[1];
        ArrayList<Population> popList = new ArrayList<>();
        File f = new File(inputFileNamePath);
        double minFit = Double.MAX_VALUE;
        double maxFit = Double.MIN_VALUE;
        if (f.isDirectory())
        {
            if (fromGeneration <= -1 || toGeneration <= -1)
            {
                logger.log(state, logID, "Generation range is invalid: " + fromGeneration
                        + " to " + toGeneration + "\n");
                state.output.fatal("Generation range is invalid: " + fromGeneration + " to "
                        + toGeneration);
            }
            for (int i = 0; i < numGenerations; i++)
            {
                Population p = PopulationUtils.loadPopulation(
                                Paths.get(inputFileNamePath, "population.gen." + i + ".bin").toFile());
                PopulationUtils.sort(p);
                double fit = p.subpops[0].individuals[0].fitness.fitness();
                if (fit > maxFit)
                    maxFit = fit;
                if (fit < minFit)
                    minFit = fit;
                popList.add(p);
            }
        } else
        {
            fromGeneration = 0;
            toGeneration = 0;
            popList.add(PopulationUtils.loadPopulation(inputFileNamePath));
        }

        return popList;
    }

//    /**
//     * Checks if the given individual is correct or acceptable or not.
//     * @param ind an individual to consider
//     * @return {@code true} if the given individual is OK.
//     */
//    private boolean checkIndividual(Individual ind)
//    {
//        if(!(ind instanceof TLGPIndividual))
//        {
//            System.err.println("WARNING: Found and object in the saved population "
//                    + " file that is not of type GPIndividual:" + ind.getClass()+ " The individual is ignored.");
//            logger.log(state, logID,
//                    "WARNING: Found and object in the saved population file"
//                            + " that is not of type GPIndividual:"
//                            + ind.getClass() + " The individual is ignored."
//                            + "\n");
//            return false;
//        }
//
//        if( !((TLGPIndividual)ind).isTested())
//        {
//            System.err.println("WARNING: The given individual is not evaluated on the test domain.");
//            logger.log(state, logID, "WARNING: The given individual is not evaluated on the test domain. " +
//                    "The individual is ignored." + "\n");
//            return false;
//        }
//
//        return true;
//    }

    private void processPopulation(Population pop)
    {
        assert pop != null;
        GPIndividual[] niches = applyNiche(pop.subpops[0].individuals);
        learnFrom(niches);
    }

    /**
     * Applies a simple niching algorithm on the given population and returns a new array that contains the individuals that
     * survived the niching.
     *
     * @param pop the population to apply niching on. The function will sort this array based on the fitness of individuals.
     */
    private GPIndividual[] applyNiche(Individual[] pop)
    {
        assert pop != null && pop.length > 0;
        ArrayList<GPIndividual> retval = new ArrayList<>();
        PopulationUtils.sort(pop);
        ArrayList<GPIndividual> niche = new ArrayList<>();
        double nicheCenter = pop[0].fitness.fitness();
        niche.add((GPIndividual) pop[0]);
        for (int i = 1; i < pop.length; i++)
        {
            Individual individual = pop[i];
            if (Math.abs(nicheCenter - individual.fitness.fitness()) <= fitnessNicheRadius)
            {
                niche.add((GPIndividual) individual);
            }
            else
            {
                // Found a new niche
                nicheCenter = individual.fitness.fitness();
                niche.sort(Comparator.comparingInt(ind -> ind.trees[0].child.depth()));
                retval.add(niche.get(0));
                niche.clear();
                niche.add((GPIndividual) individual);
            }
        }

        return retval.toArray(new GPIndividual[0]);
    }

    private void learnFrom(Individual[] individuals)
    {
        String[] terminals = UCARPUtils.getTerminalNames();
        String[] functions = UCARPUtils.getFunctionSet();
        if(learner == null)
            learner = new FrequencyLearner(state, 0, functions, terminals, this.lr, this.sampleSize, this.tournamentSize);

        if(tree == null)
            tree = new PPTree(learner, functions, terminals);
        GPIndividual[] inds = new GPIndividual[individuals.length];


        inds = Arrays.copyOf(individuals, inds.length, GPIndividual[].class);
        learner.adaptTowards(tree, inds, 0);
    }

    private void saveResults(String outputFileNamePath) throws IOException
    {
        File out = new File(outputFileNamePath);
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(out)));
//            os.writeDouble(minFit);
//            os.writeDouble(maxFit);
        os.writeObject(tree);
        os.close();
    }

    public void main(String[] args)
    {
        if(args.length < 3 )
        {
            System.err.println("Invalid number of arguments. Usage: ExtractPPT "
                    + " <test param file> <input population file/folder>"
                    + " <output file> [<EJC params>...]");
            // The reason that I am not using a parameter file instead of command line arguments is
            // that first, I don't like param files; second, I want to use the same param file that
            // is used during testing without modifying it; third, the experiments already have
            // several param files that make correct management of them difficult; fifth, it is
            // easier to modify command line arguments via shell scripts.
            return;
        }

        loadECJ(args[0], Arrays.copyOfRange(args, 3, args.length));

//        HashCalculator hs = new AlgebraicHashCalculator(state, 0, 100003621);

        String outputFileNamePath = args[2];
        try
        {
            String inputFileNamePath = args[1];
            ArrayList<Population> popList = readPopulation(inputFileNamePath);

            logger.log(state, logID, "population is loaded\n");
            for(int gen = fromGeneration; gen <= toGeneration; gen++)
            {
                Population pop = popList.get(gen);
                processPopulation(pop);
            }

            saveResults(outputFileNamePath);
            String learnedTree = tree.toString();
            logger.log(state, logID, "Finished analyzing. Learned tree: " + "\n" + learnedTree + "\n");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("\nFile not found: " + e.getMessage());
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("A class not found exception occurred. Could not find class of the"
                    + "object in the saved file. Is the file OK?");
            e.printStackTrace();
        }
        catch(InvalidObjectException e)
        {
            System.err.println("File contains objects that are not of type GPIndividual.");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }
}


public class ExtractPPT
{
    public static void main(String[] args)
    {
        Extractor extractor = new Extractor();
        extractor.main(args);
    }
}
