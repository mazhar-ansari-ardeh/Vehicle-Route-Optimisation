package tl.gphhucarp;

import ec.gp.GPNode;
import gphhucarp.gp.UCARPPrimitiveSet;
import gphhucarp.gp.terminal.feature.*;
import gputils.function.*;
import gputils.terminal.DoubleERC;
import gputils.terminal.TerminalERCUniform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A helper class that defines utility methods that can facilitate the working with UCARP.
 */
public class UCARPUtils
{
    /**
     * Returns the name of GP terminals for solving UCARP. The names are abbreviated and sorted alphabetically. The returned
     * list also contains the 'ERC' terminal.
     * @return a string array of GP terminal names.
     */
    public static String[] getTerminalNames()
    {
        List<GPNode> set = UCARPPrimitiveSet.wholeTerminalSet().getList();

        ArrayList<String> strs = new ArrayList<>();
        set.forEach(i -> strs.add(i.toString()));
//        Comparator<? super E> comparator = ;
        strs.add("ERC");
        strs.sort(Comparator.naturalOrder());
        String[] retval = new String[strs.size()];
        retval = strs.toArray(retval);
        return retval;

//      return new String[]{"CFH", "CFR1", "CR", "CTD", "CTT1", "DEM", "DEM1", "ERC", "FRT", "FULL", "FUT", "RQ", "RQ1", "SC"};
    }

    /**
     * Returns the name of functions for solving UCARP.
     * @return a string array of GP functions.
     */
    public static String[] getFunctionSet()
    {
        return new String[] {"+", "-", "*", "/", "if", "max", "min"};
    }

    /**
     * Creates a new UCARP node based on its given name. In case the given name is 'ERC', a new ERC node will be returned and
     * the value of the created node will be the value given with the parameter {@code ercValue}.
     * @param primitiveName the name of the desired primitive. This parameter cannot be {@code null} or empty.
     * @param ercValue the value to be set to the ERC node if the desired node is of type 'ERC'.
     * @return A GPNode object of the desired type.
     * @throws IllegalArgumentException if the primitive name is {@code null}, empty or is not recognised.
     */
    public static GPNode createPrimitive(String primitiveName, double ercValue)
    {
        if(primitiveName == null || primitiveName.isEmpty())
            throw new IllegalArgumentException("Terminal name cannot be null or empty");

        switch (primitiveName.toLowerCase())
        {
            case "cfd":
                return new CostFromDepot();
            case "cfh":
                return new CostFromHere();
            case "cfr1":
                return new CostFromRoute1();
            case "cr":
                return new CostRefill();
            case "ctd":
                return new CostToDepot();
            case "ctt1":
                return new CostToTask1();
            case "dc":
                return new DeadheadingCost();
            case "dem":
                return new Demand();
            case "dem1":
                return new Demand1();
            case "frt":
                return new FractionRemainingTasks();
            case "full":
                return new Fullness();
            case "fut":
                return new FractionUnassignedTasks();
            case "rq":
                return new RemainingCapacity();
            case "rq1":
                return new RemainingCapacity1();
            case "sc":
                return new ServeCost();
            case "erc":
                TerminalERCUniform retval = new TerminalERCUniform();
                retval.children = new GPNode[0];
                DoubleERC terminal = new DoubleERC();
                terminal.value = ercValue;
                retval.setTerminal(terminal);
                return retval;

            case "+":
                return new Add();
            case "-":
                return new Sub();
            case "*":
                return new Mul();
            case "/":
                return new Div();
            case "min":
                return new Min();
            case "max":
                return new Max();
            case "if":
                return new If();
            default:
                throw new IllegalArgumentException("The given terminal name is not recognised: " + primitiveName);
        }
    }
}
