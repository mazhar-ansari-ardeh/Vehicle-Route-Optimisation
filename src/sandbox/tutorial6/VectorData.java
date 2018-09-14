package sandbox.tutorial6;

import java.util.Arrays;

import ec.gp.GPData;

public class VectorData extends GPData
{

	private static final long serialVersionUID = 1L;
	
	public Double[] vector;
	public double mDataClass;
	
	public void resetVector(Double[] newVector)
	{
		vector = Arrays.copyOf(newVector, newVector.length);
	}
	
	public void copyTo(final GPData other)
	{
		if(!(other instanceof VectorData))
			return; 
		
		if(this.vector == null)
		{
			((VectorData)other).vector = null; 
		}
		else
		{
			((VectorData)other).vector = Arrays.copyOf(this.vector, this.vector.length);
			((VectorData)other).mDataClass = mDataClass; 
		}
	}

}
