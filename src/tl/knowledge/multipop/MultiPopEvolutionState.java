package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.PoolFilter;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.GPHHEvolutionState;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tl.gp.TLGPIndividual;
import tl.gp.entropy.Entropy;
import tl.gp.fitness.NormalisedMultiObjectiveFitness;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MultiPopEvolutionState extends GPHHEvolutionState implements MultiPopDMSSaver
{
    private static final String P_MULT_POP_STATE = "multipop-state";
    protected List<TreeMap<Individual, List<DecisionSituation>>> seenSituations = new ArrayList<>();

    private final List<ReactiveDecisionSituation> initialSituations = new ArrayList<>();

    /**
     * If {@code true}, the seen decision situations will be saved.
     */
    public final String P_SAVE_DMS = "save-dms";
    private boolean saveDMS = true;

    /**
     * The directory to which the population is logged at the end of each generation.
     */
    public static final String P_POP_LOG_PATH = "pop-log-path";
    private String popLogPath;

    protected PoolFilter filter;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        if(base == null)
            base = new Parameter(P_MULT_POP_STATE);
        Parameter p = new Parameter(P_SAVE_DMS);
        saveDMS = parameters.getBoolean(p, null,true);
        super.setup(state, base);

        int numSubpop = this.parameters.getInt(new Parameter("pop.subpops"), null);
        for(int i = 0; i < numSubpop; i++)
        {
            seenSituations.add(new TreeMap<>());
        }

        p = new Parameter("eval.problem.pool-filter");
        filter = (PoolFilter) (state.parameters.getInstanceForParameter(p, null, PoolFilter.class));
        entropy = new Entropy(getInitialSituations().subList(0, 20));

        popLogPath = state.parameters.getString(base.push(P_POP_LOG_PATH), null);
        if(popLogPath == null)
        {
            state.output.fatal("Population log path cannot be null");
            return;
        }
        state.output.warning("Population log path: " + popLogPath);
    }

    @Override
    public void initStatFile() {
//		statFile = new File(statDir == null ? "." : statDir, "job." + jobSeed + ".stat.csv");
        // Originally, the following line was the line above.
        for(int i = 0; i < this.population.subpops.length; i++)
        {
            Parameter p = new Parameter("stat.subpop." + i +".file");
            File evoStatFile = this.parameters.getFile(p, null);
            File evoStatDir = evoStatFile.getParentFile();
            File statFile = new File(evoStatDir, evoStatFile.getName().replaceAll("\\.out", "")+".csv");
            boolean mkdirs = statFile.getParentFile().mkdirs();
            if(!mkdirs)
                System.out.println("Failed to create the statistics directory. Does it exist?");
            if (statFile.exists()) {
                statFile.delete();
            }

            writeStatFileTitle(statFile);
            statFiles.add(statFile);
        }
    }

    List<File> statFiles = new ArrayList<>();

    public void writeStatFileTitle(File statFile) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(statFile));
            writer.write("Gen,Time,ProgSizeMean,ProgSizeStd,FitMean,FitStd,Entropy");
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<Map<String, DescriptiveStatistics>> statisticsMp;
    public void setupStatistics()
    {
        statisticsMp = new ArrayList<>();
        for (int i = 0; i < this.population.subpops.length; i++) {
            Map<String, DescriptiveStatistics> statisticsMap = new HashMap<>();
            statisticsMap.put(POP_PROG_SIZE, new DescriptiveStatistics());
            statisticsMap.put(POP_FITNESS, new DescriptiveStatistics());
            statisticsMp.add(statisticsMap);
        }
    }

    public void calcStatistics(Map<String, DescriptiveStatistics> statisticsMap) {
        statisticsMap.get(POP_PROG_SIZE).clear();
        statisticsMap.get(POP_FITNESS).clear();

        for (Individual indi : population.subpops[0].individuals) {
            int progSize = ((GPIndividual)indi).trees[0].child.numNodes(GPNode.NODESEARCH_ALL);
            statisticsMap.get(POP_PROG_SIZE).addValue(progSize);
            double fitness = indi.fitness.fitness();
            if (fitness != Double.POSITIVE_INFINITY && fitness != Double.NEGATIVE_INFINITY)
                statisticsMap.get(POP_FITNESS).addValue(fitness);
        }
    }

    Entropy entropy;

    public void writeToStatFile()
    {
        for(int i = 0; i < this.population.subpops.length; i++)
        {
            Map<String, DescriptiveStatistics> statMap = statisticsMp.get(i);
            calcStatistics(statisticsMp.get(i));

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(statFiles.get(i), true));
                writer.write(generation + "," + duration +
                        "," + statMap.get(POP_PROG_SIZE).getMean() +
                        "," + statMap.get(POP_PROG_SIZE).getStandardDeviation() +
                        "," + statMap.get(POP_FITNESS).getMean() +
                        "," + statMap.get(POP_FITNESS).getStandardDeviation() +
                        "," + entropy.entropy(population.subpops[i].individuals, 0, filter) + " " + entropy.getnClusters() + " "  + entropy.getnNoise()
                );
                writer.newLine();
                writer.close();
                logEntropy(entropy, i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateSeenSituations(Individual ind, List<DecisionSituation> situations)
    {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public List<ReactiveDecisionSituation> getAllSeenSituations()
    {
        throw new RuntimeException("Not supported.");
    }

    @Override
    public void updateSeenSituations(int subPop, Individual ind, List<DecisionSituation> situations)
    {
        if(!this.saveDMS)
            return;

        if(seenSituations.size() > 1)
            return;

        List<DecisionSituation> clonedSituations = new ArrayList<>(situations.size());
        situations.forEach(situation -> clonedSituations.add(new ReactiveDecisionSituation((ReactiveDecisionSituation) situation)));
        TreeMap<Individual, List<DecisionSituation>> theMap = seenSituations.get(subPop);
        theMap.put(ind, clonedSituations);
        if(theMap.size() > 5)
        {
            theMap.remove(theMap.lastKey());
        }
    }

    @Override
    public List<ReactiveDecisionSituation> getAllSeenSituations(int subPop)
    {
        ArrayList<ReactiveDecisionSituation> retval = new ArrayList<>();
        Set<Map.Entry<Individual, List<DecisionSituation>>> a = seenSituations.get(subPop).entrySet();
        for(Map.Entry<Individual, List<DecisionSituation>> e : a)
        {
            List<DecisionSituation> situations = e.getValue();
            for(DecisionSituation situation : situations)
            {
                ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
                if(rds.getPool().size() >= 2) // greater than two because the correlation-based phenotypics require it.
                    retval.add(rds);
            }
        }

        for(Individual ind : seenSituations.get(subPop).keySet())
        {
            List<DecisionSituation> situations = seenSituations.get(subPop).get(ind);
            for(DecisionSituation situation : situations)
            {
                ReactiveDecisionSituation rds = (ReactiveDecisionSituation)situation;
                if(rds.getPool().size() > 0)
                    retval.add(rds);
            }
        }

        return retval;
    }

    @Override
    public int evolve() {

        if (generation > 0)
            output.message("Generation " + generation);

        // EVALUATION
        statistics.preEvaluationStatistics(this);
        evaluator.evaluatePopulation(this);
//	    clear();
        statistics.postEvaluationStatistics(this);

        finish = util.Timer.getCpuTime();
        duration = 1.0 * (finish - start) / 1000000000;

        writeToStatFile();
        logPopulation(population);

        start = util.Timer.getCpuTime();

        // SHOULD WE QUIT?
        if (evaluator.runComplete(this) && quitOnRunComplete) {
            output.message("Found Ideal Individual");
            return R_SUCCESS;
        }

        // SHOULD WE QUIT?
        if (generation == numGenerations-1) return R_FAILURE;

        // PRE-BREEDING EXCHANGE
        if (exchangePopulationPreBreeding()) return R_SUCCESS;

        // BREEDING
        breed();

        // POST-BREEDING EXCHANGING
        exchangePopulationPostBreeding();

        // Generate new instances if needed
        rotateEvalModel();

        // INCREMENT GENERATION AND CHECKPOINT
        generation++;
        doCheckpoit();

        return R_NOTDONE;
    }

    @Override
    public List<ReactiveDecisionSituation> getInitialSituations()
    {
        return initialSituations;
    }

    @Override
    public void addToInitialSituations(DecisionSituation initialSituation)
    {
        if(this.initialSituations.size()> 10000)
            return;
        this.initialSituations.add((ReactiveDecisionSituation) initialSituation);
    }

    @Override
    public void setDMSSavingEnabled(boolean enabled)
    {
        this.saveDMS = enabled;
    }

    @Override
    public boolean isDMSSavingEnabled()
    {
        return this.saveDMS;
    }

    @Override
    protected void rotateEvalModel()
    {
        if (rotateEvalModel) {
            MultiPopReactiveGPHHProblem problem = (MultiPopReactiveGPHHProblem)evaluator.p_problem;
            problem.rotateEvaluationModel();
        }
    }

    private void logEntropy(Entropy entropy, int subpop)
    {
        int popLogID = setupLogger(this, new File(popLogPath, String.format("pop/Cluster.%d.%d.csv", subpop, generation)).getAbsolutePath());
        log(this, popLogID, entropy.toString());
        closeLogger(this, popLogID);
    }

    protected void logPopulation(Population population)
    {
        assert population != null;
        for(int subpop = 0; subpop < population.subpops.length; subpop++)
        {
            Individual[] pop = population.subpops[subpop].individuals;
            assert pop != null;

            int popLogID = setupLogger(this, new File(popLogPath, String.format("pop/Pop.%d.%d.csv", subpop, generation)).getAbsolutePath());
            log(this, popLogID, "Origin,Fitness,Tree\n");
            Arrays.stream(pop).map(i -> (TLGPIndividual) i).sorted(Comparator.comparingDouble(j -> j.fitness.fitness())).forEach(
                    i -> log(this, popLogID,
                            i.getOrigin() + "," + i.fitness.fitness() + "," + i.trees[0].child.makeLispTree() + "\n")
            );
            closeLogger(this, popLogID);
        }
    }
}