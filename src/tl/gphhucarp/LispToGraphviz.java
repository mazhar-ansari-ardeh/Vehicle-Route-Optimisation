package tl.gphhucarp;

import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

public class LispToGraphviz
{
    public static void main(String[] args)
    {
        String lispString = "(- (* (min (* (* (* (/ SC 0.5742833472668298) (+ CTD FRT)) (/ 0.012840418778625184 DEM)) (+ (* (* RQ CFH) CFH) RQ)) SC) (/ (* (+ (* (/ 0.012840418778625184 DEM) CFH) (+ 0.9262409369708411 (* RQ CFH))) CFH) FULL)) (/ (- (max (- (min RQ (- RQ CR)) (- (+ CTT1 RQ) (max CTT1 FULL))) (+ CTD FRT)) (/ (+ SC (+ CFH (* RQ CFH))) (* (max FRT CR) CFH))) (max (* (* (min SC (* SC CFH)) CFH) (+ (/ 0.012840418778625184 DEM) (+ CTD FRT))) (+ CTD FRT))))";

        String gvString = LispUtils.parseExpression(lispString, UCARPPrimitiveSet.wholePrimitiveSet()).child.makeGraphvizTree();

        System.out.println(gvString);
    }
}
