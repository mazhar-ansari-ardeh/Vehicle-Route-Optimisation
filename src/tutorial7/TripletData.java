package tutorial7;

import ec.gp.GPData;

public class TripletData extends GPData
{

	private static final long serialVersionUID = 1L;

	private double x = -1;

	private double y = -1;

	private double z = -1;

	private double result = -1;

	private boolean resultSet = false;

	public TripletData()
	{}

	public TripletData(double x, double y, double z)
	{
		super();
		this.x = x;
		this.y = y;
		this.z = z;
		resultSet = false;
	}

	public double getX() {
		return x;
	}

//	public void setX(double x) {
//		this.x = x;
//	}

	public double getY() {
		return y;
	}

//	public void setY(double y) {
//		this.y = y;
//	}

	public double getZ() {
		return z;
	}

//	public void setZ(double z) {
//		this.z = z;
//	}

	public double getResult() {
		return result;
	}

	public void setResult(double result) {
		this.result = result;
		resultSet = true;
	}

	public void copyTo(final GPData other)
	{
		if(!(other instanceof TripletData))
			return;

		((TripletData)other).x = x;
		((TripletData)other).y = y;
		((TripletData)other).z = z;
		((TripletData)other).result = result;
		((TripletData)other).resultSet = resultSet;
	}

	@Override
	public String toString()
	{
		return "(" + x + ", " + y + ", " + z + "; " + result + ": " + (resultSet ? "set":"not set") + ")";
	}
}
