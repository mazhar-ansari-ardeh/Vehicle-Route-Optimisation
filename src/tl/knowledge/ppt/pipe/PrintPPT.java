package tl.knowledge.ppt.pipe;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class PrintPPT
{
    private static PPTree loadPPT(String knowFile) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oin = new ObjectInputStream(new FileInputStream(knowFile));
        PPTree tree = (PPTree) oin.readObject();
        oin.close();
        return tree;
    }
    public static void main(String[] args)
    {   ///am/vuwstocoisnrin1.vuw.ac.nz/grid-solar/sgeusers/mazhar/gdb1.vs5.gdb1.vt6:gen_50/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1/2/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1.ppt
        ///am/vuwstocoisnrin1.vuw.ac.nz/grid-solar/sgeusers/mazhar/gdb1.vs5.gdb1.vt6:gen_50/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1/2/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1.ppt
        String fileName = "/am/vuwstocoisnrin1.vuw.ac.nz/grid-solar/sgeusers/mazhar/gdb1.vs5.gdb1.vt6:gen_50/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1/3/PPTFreqLearning:percent_0.5:nrad_-1:ncap_2:gens_49_49:lr_0.8:ss_100:ts_-1.ppt";
        if(args.length > 1)
        {
            fileName = args[0];
        }

        try
        {
            PPTree ppTree = loadPPT(fileName);
            String tree = ppTree.toGVString(2);
            System.out.println(tree);
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }

    }
}
