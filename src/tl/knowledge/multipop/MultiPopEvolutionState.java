package tl.knowledge.multipop;

import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.util.Parameter;
import gphhucarp.decisionprocess.DecisionSituation;
import gphhucarp.decisionprocess.reactive.ReactiveDecisionSituation;
import gphhucarp.gp.GPHHEvolutionState;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tl.gp.fitness.NormalisedMultiObjectiveFitness;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MultiPopEvolutionState extends GPHHEvolutionState implements MultiPopDMSSaver
{
    protected List<TreeMap<Individual, List<DecisionSituation>>> seenSituations = new ArrayList<>();

    private final List<ReactiveDecisionSituation> initialSituations = new ArrayList<>();

    /**
     * If {@code true}, the seen decision situations will be saved.
     */
    public final String P_SAVE_DMS = "save-dms";
    private boolean saveDMS = true;

    @Override
    public void setup(EvolutionState state, Parameter base)
    {
        Parameter p = new Parameter(P_SAVE_DMS);
        saveDMS = parameters.getBoolean(p, null,true);
        super.setup(state, base);

        int numSubpop = this.parameters.getInt(new Parameter("pop.subpops"), null);
        for(int i = 0; i < numSubpop; i++)
        {
            seenSituations.add(new TreeMap<>());
        }
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
            writer.write("Gen,Time,ProgSizeMean,ProgSizeStd,FitMean,FitStd");
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
                        "," + statMap.get(POP_FITNESS).getStandardDeviation()
                );
                writer.newLine();
                writer.close();
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
}