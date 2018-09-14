package tutorial7.problems.regression;

import java.util.Arrays;

import ec.gp.GPData;

public class VectorData extends GPData
{

	private static final long serialVersionUID = 1L;

	private double[] data;

	private double result = -1;

	private boolean resultSet = false;

	public VectorData()
	{}

	public VectorData(double... data)
	{
		super();
		if(data == null || data.length == 0)
			throw new IllegalArgumentException("Data cannot be null or empty.");

		this.data = Arrays.copyOf(data, data.length);
		resultSet = false;
	}

	public double get(int i)
	{
		return this.data[i];
	}

	public double getResult()
	{
		return result;
	}

	public void setResult(double result) {
		this.result = result;
		resultSet = true;
	}

	public void copyTo(final GPData other)
	{
		if(!(other instanceof VectorData))
			return;

		VectorData vother = (VectorData)other;
		vother.data = Arrays.copyOf(data, data.length);

		vother.result = result;
		vother.resultSet = resultSet;
	}

	@Override
	public String toString()
	{
		String ret = "(";
		for(int i = 0; i < data.length - 1; i++)
			ret += (data[i] + ", ");
		ret += (data[data.length - 1] + ")");

		return ret + "; " + result + ": " + (resultSet ? "set":"not set") + ")";
	}
}
