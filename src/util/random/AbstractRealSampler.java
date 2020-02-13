package util.random;

import org.apache.commons.math3.random.RandomDataGenerator;

/**
 *
 * The abstract property of a distribution.
 *
 * @author gphhucarp
 *
 */

abstract public class AbstractRealSampler {

	abstract public double next(RandomDataGenerator rdg);

	abstract public void setLower(double lower);
	abstract public void setUpper(double upper);
	abstract public void setMean(double mean);
	abstract public double getMean();

	public Object clone()
	{
		try
		{
			return super.clone();
		} catch (CloneNotSupportedException e)
		{
			e.printStackTrace();
			throw new RuntimeException(e); // This should not happen.
		}
	}
}
