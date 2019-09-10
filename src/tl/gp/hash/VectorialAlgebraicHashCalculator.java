package tl.gp.hash;

import ec.EvolutionState;
import ec.gp.GPNode;
import gputils.terminal.DoubleERC;
import gputils.terminal.TerminalERCUniform;

import java.util.ArrayList;

import java.util.Arrays;

/**
 *
 */
public class VectorialAlgebraicHashCalculator implements HashCalculator
{
    private final EvolutionState state;


    private final int threadNum;

    /**
     * The upper bound for the random numbers that are generated and assigned to the terminals. A negative or a zero value
     * indicates the random numbers are unbounded.
     */
    private final int randBound;

    private double[] SCHash;
    private double[] CFDHash;
    private double[] CFHHash;
    private double[] CTDHash;
    private double[] CRHash;
    private double[] DCHash;
    private double[] DEMHash;
    private double[] RQHash;
    private double[] FULLHash;
    private double[] FRTHash;
    private double[] FUTHash;
    private double[] CFR1Hash;
    private double[] CTT1Hash;
    private double[] DEM1Hash;

    private int hashOrder;

    private ArrayList<Integer> seenNumbers = new ArrayList<>();

    public VectorialAlgebraicHashCalculator(EvolutionState eState, int threadNumber, int hashOrder, int randBound)
    {
        if (eState == null || eState.random == null)
            throw new IllegalArgumentException("State or its random generator is null");
        if(randBound <= 0)
            throw new IllegalArgumentException("Bound of the random number must be a positive number");
        this.state = eState;
        this.threadNum = threadNumber;
        this.hashOrder = hashOrder;
        this.randBound = randBound;

        SCHash = nextRand();
        CFDHash = nextRand();
        CFHHash = nextRand();
        CTDHash = nextRand();
        CRHash = nextRand();
        DCHash = nextRand();
        DEMHash = nextRand();
        RQHash = nextRand();
        FULLHash = nextRand();
        FRTHash = nextRand();
        FUTHash = nextRand();
        CFR1Hash = nextRand();
        CTT1Hash = nextRand();
        DEM1Hash = nextRand();
    }

    private double[] nextRand()
    {
        double[] retval = new double[hashOrder];

        for (int i = 0; i < hashOrder; i++)
        {
            int rnd = (int) (state.random[threadNum].nextDouble() * randBound);

            while (seenNumbers.contains(rnd))
                rnd = state.random[threadNum].nextInt();

            seenNumbers.add(rnd);
            retval[i] = rnd;
        }
        return retval;
    }

    private double[] hashOf(TerminalERCUniform t)
    {
        String name = t.getTerminal().name();
        switch (name)
        {
            case "SC":
                return SCHash;
            case "CFD":
                return CFDHash;
            case "CFH":
                return CFHHash;
            case "CTD":
                return CTDHash;
            case "CR":
                return CRHash;
            case "DC":
                return DCHash;
            case "DEM":
                return DEMHash;
            case "RQ":
                return RQHash;
            case "FULL":
                return FULLHash;
            case "FRT":
                return FRTHash;
            case "FUT":
                return FUTHash;
            case "CFR1":
                return CFR1Hash;
            case "CTT1":
                return CTT1Hash;
            case "DEM1":
                return DEM1Hash;
            case "ERC":
                double value = ((DoubleERC) t.getTerminal()).value;
                double[] values = new double[hashOrder];
                for (int i = 0; i < hashOrder; i++) values[i] = value;
                return values;
            // return hashOf(value);
            default:
                throw new RuntimeException("Received an unknown terminal to hash: " + name);
        }
    }

    @Override
    public int hashOfTree(GPNode tree)
    {
        double[] hashes = hashsOfTree(tree);
        System.out.println(Arrays.toString(hashes));

        return Arrays.hashCode(hashsOfTree(tree));
    }

    private double[] hashsOfTree(GPNode tree)
    {
        if (tree.children == null || tree.children.length == 0)
            return hashOf((TerminalERCUniform) tree);
        double[] lch = hashsOfTree(tree.children[0]); // left child hash
        double[] rch = hashsOfTree(tree.children[1]);
        double[] retval = new double[hashOrder];
        for (int i = 0; i < hashOrder; i++)
        {
            switch (tree.toString())
            {
                case "+":
                    retval[i] = lch[i] + rch[i];
                    break;
                case "-":
                    retval[i] = lch[i] - rch[i];
                    break;
                case "*":
                    retval[i] = lch[i] * rch[i];
                    break;
                case "/":
                    retval[i] = lch[i] / rch[i];
                    break;
                case "min":
                    retval[i] = Math.min(lch[i], rch[i]);
                    break;
                case "max":
                    retval[i] = Math.max(lch[i], rch[i]);
                    break;
                default:
                    throw new RuntimeException("Received an unknown terminal type: " + tree.toString());
            } // switch
        } // for
        return retval;
    }

}


