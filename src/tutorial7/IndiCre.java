package tutorial7;

import java.util.ArrayList;

import ec.gp.GPIndividual;

public class IndiCre 
{
	public static ArrayList<GPIndividual> Break(GPIndividual  ind)
	{
		ArrayList<GPIndividual> retval = new ArrayList<>();
		if(ind == null)
			return retval; 
		
		if(ind.trees[0].child.children != null 
			&& ind.trees[0].child.children.length >= 2)
		{
			for(int i = 0; i < ind.trees[0].child.children.length; i++)
			{
				GPIndividual x = ind.lightClone();
				x.trees[0].child = x.trees[0].child.children[i];
				x.trees[0].child.parent = x.trees[0];
				
				retval.add(x);
			}

			ind.trees[0].child.children = null;
		}
		
		return retval; 
	}
}
