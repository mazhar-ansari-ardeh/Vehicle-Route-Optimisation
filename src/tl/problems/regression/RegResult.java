package tl.problems.regression;


import ec.Fitness;
import ec.Problem;
import ec.gp.GPTree;
import ec.multiobjective.MultiObjectiveFitness;
import gputils.terminal.PrimitiveSet;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A GP result is a class that stores the information read from an out.stat file produced by a GP run.
 * It includes
 *  - a list of solutions (best individuals), one per generation.
 *  - a list of training fitnesses, each for a solution.
 *  - a list of demo fitnesses, each for a solution.
 *  - a best solution according to the training fitness.
 *  - the training fitness of the best solution.
 *  - the demo fitness of the best solution.
 *  - the time statistics, i.e. the time spent for each generation.
 */

public class RegResult
{
    private List<String> expressions;
    private List<GPTree> solutions;
    private List<Fitness> trainFitnesses;
    private List<Fitness> testFitnesses;
    private String bestExpression;
    private GPTree bestSolution;
    private Fitness bestTrainFitness;
    private Fitness bestTestFitness;
    private DescriptiveStatistics timeStat;

    public RegResult() {
        expressions = new ArrayList<>();
        solutions = new ArrayList<>();
        trainFitnesses = new ArrayList<>();
        testFitnesses = new ArrayList<>();
    }

    public List<String> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<String> expressions) {
        this.expressions = expressions;
    }

    public String getBestExpression() {
        return bestExpression;
    }

    public void setBestExpression(String bestExpression) {
        this.bestExpression = bestExpression;
    }

    public List<GPTree> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<GPTree> solutions) {
        this.solutions = solutions;
    }

    public List<Fitness> getTrainFitnesses() {
        return trainFitnesses;
    }

    public void setTrainFitnesses(List<Fitness> trainFitnesses) {
        this.trainFitnesses = trainFitnesses;
    }

    public List<Fitness> getTestFitnesses() {
        return testFitnesses;
    }

    public void setTestFitnesses(List<Fitness> testFitnesses) {
        this.testFitnesses = testFitnesses;
    }

    public GPTree getBestSolution() {
        return bestSolution;
    }

    public void setBestSolution(GPTree bestSolution) {
        this.bestSolution = bestSolution;
    }

    public Fitness getBestTrainFitness() {
        return bestTrainFitness;
    }

    public void setBestTrainFitness(Fitness bestTrainFitness) {
        this.bestTrainFitness = bestTrainFitness;
    }

    public Fitness getBestTestFitness() {
        return bestTestFitness;
    }

    public void setBestTestFitness(Fitness bestTestFitness) {
        this.bestTestFitness = bestTestFitness;
    }

    public DescriptiveStatistics getTimeStat() {
        return timeStat;
    }

    public void setTimeStat(DescriptiveStatistics timeStat) {
        this.timeStat = timeStat;
    }

    public GPTree getSolutionAtGen(int gen) {
        return solutions.get(gen);
    }

    public Fitness getTrainFitnessAtGen(int gen) {
        return trainFitnesses.get(gen);
    }

    public Fitness getTestFitnessAtGen(int gen) {
        return testFitnesses.get(gen);
    }

    public double getTimeAtGen(int gen) {
        return timeStat.getElement(gen);
    }

    public void addExpression(String expression) {
        expressions.add(expression);
    }

    public void addSolution(GPTree solution) {
        solutions.add(solution);
    }

    public void addTrainFitness(Fitness fitness) {
        trainFitnesses.add(fitness);
    }

    public void addTestFitness(Fitness fitness) {
        testFitnesses.add(fitness);
    }

    public static RegResult readFromFile(File file,
                                        Problem problem)
    {
    	return readSimpleSolutionFromFile(file, problem);
    }

    public static RegResult readSimpleSolutionFromFile(File file,
                                                      Problem problem)
    {
        // ReactiveGPHHProblem prob = (ReactiveGPHHProblem)problem;

        RegResult result = new RegResult();

        String line;
        Fitness fitness = null;
        GPTree solution = null;
        String expression = "";

        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            while (!(line = br.readLine()).equals("Best Individual of Run:"))
            {
                if (line.startsWith("Generation"))
                {
                    br.readLine();
                    br.readLine();
                    br.readLine();
                    line = br.readLine();
                    fitness = readFitnessFromLine(line);
                    br.readLine();
                    expression = br.readLine();

                    expression = LispUtils.simplifyExpression(expression);

                    result.addExpression(expression);

//                    RoutingPolicy routingPolicy =
//                            new GPRoutingPolicy(prob.getPoolFilter(),
//                                    LispUtils.parseExpression(expression,
//                                            UCARPPrimitiveSet.wholePrimitiveSet()));

                    PrimitiveSet primitiveSet = new PrimitiveSet();
                    primitiveSet.add(new Add());
                    primitiveSet.add(new Mul());
                    primitiveSet.add(new Sub());
                    primitiveSet.add(new Div());
                    primitiveSet.add(new X());
                    primitiveSet.add(new Sin());
                    primitiveSet.add(new Cos());
                    GPTree regTree = LispUtils.parseExpression(expression, primitiveSet);

                    result.addSolution(regTree);
                    result.addTrainFitness(fitness);
                    result.addTestFitness((Fitness)fitness.clone());

                    solution = regTree;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set the best solution as the solution in the last generation
        result.setBestExpression(expression);
        result.setBestSolution(solution);
        result.setBestTrainFitness(fitness);
        result.setBestTestFitness((Fitness)fitness.clone());

        return result;
    }

    private static Fitness readFitnessFromLine(String line)
    {
        return readSimpleFitnessFromLine(line);
    }

    private static Fitness readSimpleFitnessFromLine(String line)
    {
    	String[] segments = line.split("\\[|\\]");
        double fitness = Double.valueOf(segments[1]);
        MultiObjectiveFitness f = new MultiObjectiveFitness();
        f.objectives = new double[1];
        f.objectives[0] = fitness;

        return f;
    }

    public static DescriptiveStatistics readTimeFromFile(File file)
    {
        DescriptiveStatistics generationalTimeStat = new DescriptiveStatistics();

        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            while(true) {
                line = br.readLine();

                if (line == null)
                    break;

                String[] commaSegments = line.split(",");
                generationalTimeStat.addValue(Double.valueOf(commaSegments[1]));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return generationalTimeStat;
    }
}
