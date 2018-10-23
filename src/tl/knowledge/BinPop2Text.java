package tl.knowledge;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import ec.gp.*;
import tl.knowledge.codefragment.fitted.DoubleFittedCodeFragment;

public class BinPop2Text
{
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
						tree = gind.trees[0].child.makeGraphvizTree();
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

	public static void main(String[] args) throws ClassNotFoundException, IOException
	{
		String path = "/home/mazhar/MyPhD/SourceCodes/";
		boolean printFitness = true;
		if(args.length >= 1)
			path = args[0];

		Properties p = loadSettings();
		printFitness = p.getProperty("load-fitness", "true").equals("true");
		String graphType = p.getProperty("graph-type", "dot");

		Path rootPath = Paths.get(path);
		ArrayList<Path> regularFilePaths = Files.list(rootPath)
				.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(".bin") || file.getFileName().toString().endsWith(".cf"))
				.collect(Collectors.toCollection(ArrayList::new));

		regularFilePaths.forEach(System.out::println);

		for(Path file: regularFilePaths)
		{
			process(file, printFitness, graphType);
		}
	}

}
