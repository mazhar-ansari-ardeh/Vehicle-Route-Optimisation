package tl.gphhucarp;

import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

public class LispToGraphviz
{
    public static void main(String[] args)
    {
        String lispString = "(- (max (+ (+ (min (* CR DC) (+ FULL SC)) (- (min CR (+ FULL SC)) (+ (+ FULL SC) CFR1))) (* CTD FULL)) (- (min (max FRT CFH) (- CFH CTD)) (max (/ CFH FUT) SC))) (/ (max (+ (max (/ CFH FUT) SC) (+ (+ (* CR DC) (* CFR1 CTT1)) CFR1)) (+ (min (/ CFH CTT1) (* (* CTT1 CFR1) (* CTT1 CTT1))) FUT)) (max (+ (max (max FRT CFH) (min RQ (* DEM1 DEM1))) (max (max (max FRT CFH) (min FULL DC)) CTT1)) (* (* (max (max FRT CFH) (min FULL DC)) (min RQ (* DEM1 DEM1))) (* CTD FULL)))))";

        String gvString = LispUtils.parseExpression(lispString, UCARPPrimitiveSet.wholePrimitiveSet()).child.makeGraphvizTree();

        System.out.println(gvString);
    }
}
