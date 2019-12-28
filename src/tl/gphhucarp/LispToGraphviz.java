package tl.gphhucarp;

import gphhucarp.gp.UCARPPrimitiveSet;
import gputils.LispUtils;

public class LispToGraphviz
{
    public static void main(String[] args)
    {
        String lispString = "(* (* (+ (min FULL SC) (/ (min FULL (/ (min CTD RQ) FULL)) FULL)) (max (max (min CR CFD) (min CTD RQ)) (+ (min FULL (min CR CFD)) (/ (/ DC SC) (max (- CFR1 DC) (min DC CTT1)))))) (- (max (min (min CTD (min CTD DEM)) (min (+ CFH DC) (min FRT RQ))) (* (* (max CFH (+ DEM CR)) (- (+ CFH SC) (+ FUT DC))) RQ)) (* (+ (max CR (min SC RQ)) (/ DC DEM1)) (min (min CTD DEM) (/ DC DEM1)))))";

        String gvString = LispUtils.parseExpression(lispString, UCARPPrimitiveSet.wholePrimitiveSet()).child.makeGraphvizTree();

        System.out.println(gvString);
    }
}
