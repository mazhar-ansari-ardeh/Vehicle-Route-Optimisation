package tutorial5;

import ec.gp.GPData;

public class DoubleData extends GPData
{

	private static final long serialVersionUID = 1L;
	
	public double x;
	
	public void copyTo(final GPData other)
	{
		if(!(other instanceof DoubleData))
			return; 
		
		((DoubleData)other).x = x; 
	}

}
