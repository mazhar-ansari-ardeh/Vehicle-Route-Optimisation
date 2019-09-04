package tl.gp.hash;

import ec.EvolutionState;
import ec.gp.GPNode;
import gputils.terminal.DoubleERC;
import gputils.terminal.TerminalERCUniform;

import java.util.ArrayList;

/**
 * This class implements the idea in the paper:
 * "Phillip Wong and Mengjie Zhang. 2006. Algebraic simplification of GP programs during evolution. In Proceedings of the
 * 8th annual conference on Genetic and evolutionary computation (GECCO '06)".
 */
public class AlgebraicHashCalculator implements HashCalculator {

    private final EvolutionState state;

    private final int threadNum;

    private int SCHash;
    private int CFDHash;
    private int CFHHash;
    private int CTDHash;
    private int CRHash;
    private int DCHash;
    private int DEMHash;
    private int RQHash;
    private int FULLHash;
    private int FRTHash;
    private int FUTHash;
    private int CFR1Hash;
    private int CTT1Hash;
    private int DEM1Hash;

    private int prime = 1000077157; // This is the value that the paper authors used.

    private ArrayList<Integer> seenNumbers = new ArrayList<>();

    public AlgebraicHashCalculator(EvolutionState eState, int threadNumber, int primeNumber)
    {
        if(eState == null || eState.random == null)
            throw new IllegalArgumentException("State or its random generator is null");
        state = eState;
        threadNum = threadNumber;
        prime = primeNumber;

//        SCHash = nextRand();
//        CFDHash = nextRand();
//        CFHHash = nextRand();
//        CTDHash = nextRand();
//        CRHash = nextRand();
//        DCHash = nextRand();
//        DEMHash = nextRand();
//        RQHash = nextRand();
//        FULLHash = nextRand();
//        FRTHash = nextRand();
//        FUTHash = nextRand();
//        CFR1Hash = nextRand();
//        CTT1Hash = nextRand();
//        DEM1Hash = nextRand();

        SCHash = 3;
        CFDHash = 5;
        CFHHash = 7;
        CTDHash = 11;
        CRHash = 13;
        DCHash = 15;
        DEMHash = 17;
        RQHash = 19;
        FULLHash = 21;
        FRTHash = 23;
        FUTHash = 25;
        CFR1Hash = 27;
        CTT1Hash = 29;
        DEM1Hash = 31;
    }

    private int nextRand()
    {
        int rnd = state.random[threadNum].nextInt(prime);
        while(seenNumbers.contains(rnd) == true)
            rnd = state.random[threadNum].nextInt(prime);

        seenNumbers.add(rnd);
        return rnd;
    }

    private int hashOf(TerminalERCUniform t)
    {
        String name = t.getTerminal().name();
        switch(name)
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
                double value = ((DoubleERC)t.getTerminal()).value;
                return hashOf(value);
            default:
                throw new RuntimeException("Received an unknown terminal to hash: " + name);
        }
    }

    private static int mod(int a, int b)
    {
        int ret = a % b;
        while(ret < 0)
            ret += b;

        return ret;
    }

    private static int eea(int a, int b, int p)
    {
        if(b == 0)
            return 1; // TODO: Are you sure?
        int r = 1;

        int q0 = a / b;
        r = a - q0 * b;
        int x0 = 0;
        if(r == 0)
            return x0;

        a = b;
        b = r;
        int q1 = a / b;
        r = a - q1 * b;
        int x1 = 1;
        if(r == 0)
            return x1;

        while(r != 0)
        {
            a = b;
            b = r;

            int q = a / b;
            r = a - q * b;

            int x2 = mod((x0 - x1*q0), p);
            x0 = x1;
            x1 = x2;
            q0 = q1;
            q1 = q;
        }

        int x2 = mod((x0 - x1*q0), p);
        return x2;
    }

    private int hashOf(double value)
    {
        value *= 10;
        int cd = (int) value;
        int retval = mod(cd * eea(prime, 10, prime), prime);
        return retval;
    }

    @Override
    public int hashOfTree(GPNode tree)
    {
        if(tree.children == null || tree.children.length == 0)
            return hashOf((TerminalERCUniform) tree);
        int lch = hashOfTree(tree.children[0]); // left child hash
        int rch = hashOfTree(tree.children[1]);
        if(tree.toString().equals("+"))
            return AlgebraicHashCalculator.mod(lch + rch, prime);
        if(tree.toString().equals("-"))
            return AlgebraicHashCalculator.mod(lch - rch, prime);
        if(tree.toString().equals("*"))
            return AlgebraicHashCalculator.mod(lch * rch, prime);
        if(tree.toString().equals("/"))
            return hashOf((double)lch / (double)rch);
        if(tree.toString().equals("min"))
        {
            return hashOf(AlgebraicHashCalculator.eea( AlgebraicHashCalculator.mod(lch - rch, prime), lch, prime) + rch);
            // return Math.min(lch, rch);
        }
        if(tree.toString().equals("max"))
        {
            return hashOf(AlgebraicHashCalculator.eea(AlgebraicHashCalculator.mod(lch - rch, prime), rch, prime) + lch);
            //return Math.max(lch, rch);
        }
        throw new RuntimeException("Received an unknown terminal type: " + tree.toString());
    }
}
