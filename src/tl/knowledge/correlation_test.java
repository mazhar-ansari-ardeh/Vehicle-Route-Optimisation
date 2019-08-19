package tl.knowledge;

/**
 * This is a simple and quick-fix program to test the correalation stuff that Yi wanted. This file is not important and
 * can be deleted.
 */


import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import ec.Individual;
import ec.Population;
import ec.gp.*;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import tl.gp.PopulationUtils;
import tl.knowledge.codefragment.fitted.DoubleFittedCodeFragment;

public class correlation_test
{
    private static double getBest(String path) throws IOException {
        BufferedReader ds = new BufferedReader(new FileReader(path));
        ds.readLine();
        String first = ds.readLine();
        ds.close();
        String fit = first.split(",")[7];
        return Double.parseDouble(fit);
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException
    {
        String source = "val10D-v10-writeknow";
        String target = "val10D-v10-to-9-wk-GTLKnowlege";
        boolean printFitness = true;
        double[] sourceMean = new double[30];
        double[] targetBest = new double[30];
        double[][] data = new double[30][];
        for(int run = 1; run <= 30; run++)
        { // /home/mazhar/scratch/CEC/val10D-v10-to9/1/stats/val10D-v10-to-9-wk-TLGPCriptor/test/
            String spath =  "/home/mazhar/scratch/CEC/val10D-v10-to9/" + run + "/stats/" + source + "/population.gen.49.bin";
            String tpath =  "/home/mazhar/scratch/CEC/val10D-v10-to9/" + run + "/stats/" + target + "/test/total-cost-val10D-9-0.2-0.2.csv";
            data[run-1] = new double[2];
            Population p = PopulationUtils.loadPopulation(spath);
            PopulationUtils.sort(p);
            double[] fitneses = new double[p.subpops[0].individuals.length / 2];
            for(int i = 0; i < fitneses.length; i++)
                fitneses[i] = p.subpops[0].individuals[0].fitness.fitness();

            DescriptiveStatistics stats = new DescriptiveStatistics(fitneses);
            sourceMean[run - 1] = stats.getMean();
            data[run-1][0] = sourceMean[run - 1];
            targetBest[run - 1] = getBest(tpath);
            data[run-1][1] = targetBest[run - 1];
        }
        for (int i = 0; i < sourceMean.length; i++)
            System.out.print(sourceMean[i] + ", ");
        System.out.println();
        for (int i = 0; i < targetBest.length; i++)
            System.out.print(targetBest[i] + ", ");
        System.out.println();
        PearsonsCorrelation cor = new PearsonsCorrelation();
        double r = cor.correlation(sourceMean, targetBest);
        System.out.println(r);



//        Properties p = loadSettings();
//        printFitness = p.getProperty("load-fitness", "true").equals("true");
//        String graphType = p.getProperty("graph-type", "dot");
//
//        Path rootPath = Paths.get(path);
//        ArrayList<Path> regularFilePaths = Files.list(rootPath)
//                .filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(".bin") || file.getFileName().toString().endsWith(".cf"))
//                .collect(Collectors.toCollection(ArrayList::new));
//
//        regularFilePaths.forEach(System.out::println);

//        for(Path file: regularFilePaths)
//        {
//            process(file, printFitness, graphType);
//        }
    }

    final static String SettingFile = "BinPop2Text.ini";

    static Properties loadSettings()
    {
        Properties p = new Properties();
        try(FileInputStream fin = new FileInputStream(new File(SettingFile)))
        {
            p.load(fin);

            return p;
        } catch (IOException e)
        {
            Properties def = new Properties();
            def.setProperty("load-fitness", "true");
            def.setProperty("graph-type", "dot");
            return def;
        }
    }

    private static void processCFFile(Path file, boolean printFitness, String graphType)
    {
        File outFile = new File(file.toString().replaceAll("\\.cf", ".cfind"));
        if(outFile.exists())
            outFile.delete();
        try(ObjectInputStream oos = new ObjectInputStream(new FileInputStream(file.toFile()));
            PrintWriter out = new PrintWriter(new FileOutputStream(outFile)))
        {
            System.out.println("Creating file: " + outFile.getAbsolutePath());

            while(true)
            {
                try
                {
                    Object obj = oos.readObject();
                    if(!(obj instanceof DoubleFittedCodeFragment))
                    {
                        System.out.println("Object is not instance of DoubleFittedCodeFragment: "
                                + obj.getClass().toGenericString());
                        System.out.println("Object ignored.");
                    }
                    DoubleFittedCodeFragment cf = (DoubleFittedCodeFragment)obj;
                    out.println(cf.toString());
                } catch (ClassNotFoundException e)
                {
                    System.out.println(e.toString());
                    System.out.println("Object ignored.\n");
                }
            }
        }
        catch(EOFException e)
        {
            // Do nothing. This is normal.
        }
        catch (IOException e)
        {
            System.out.println("Failed to process the file:" + file.toString()
                    + " the file ignored. Exception: " + e.toString());
        }
    }

    private static void processBinFile(Path file, boolean printFitness, String graphType)
    {
        try
        {
            ObjectInputStream dis = new ObjectInputStream((new FileInputStream(file.toFile())));
            File outFile = new File(file.toString().replaceAll("\\.bin", ".ind"));
            if(outFile.exists())
                outFile.delete();
            outFile.createNewFile();

            System.out.println("Creating file: " + outFile.getAbsolutePath());
            PrintWriter out = new PrintWriter(new FileOutputStream(outFile));

            int nsub = dis.readInt();

            for(int i = 0; i < nsub; i++)
            {
                int nind = dis.readInt();
                for(int j = 0; j < nind; j++)
                {
                    Object ind = dis.readObject();
                    if(!(ind instanceof GPIndividual))
                        continue;
                    GPIndividual gind = (GPIndividual) ind;
                    String tree = null;
                    if(graphType.equals("dot"))
                        tree = gind.trees[0].child.makeGraphvizTree().replaceAll("\\n", "");
                    else if(graphType.equals("c"))
                        tree = gind.trees[0].child.makeCTree(true, true, true);
                    else if(graphType.equals("lisp"))
                        tree = gind.trees[0].child.makeLispTree();
                    else
                        tree = gind.trees[0].child.makeLatexTree();

                    out.print(tree);
                    if(printFitness)
                        out.println(", " + gind.fitness.fitness() + "\n");
                    else
                        out.println("\n");
                    out.flush();
                }
            } // for
            out.close();
            dis.close();
        }// try
        catch(IOException | ClassNotFoundException exp)
        {
            System.out.println("Failed to process the file:" + file.toString()
                    + " the file ignored. Exception: " + exp.toString());
        }
    }

    private static void process(Path file, boolean printFitness, String graphType)
    {
        if(file.getFileName().toString().endsWith(".cf"))
        {
            processCFFile(file, printFitness, graphType);
            return;
        }
        if(file.getFileName().toString().endsWith(".bin"))
        {
            processBinFile(file, printFitness, graphType);
            return;
        }
    }


}
