package util.random;

import org.apache.commons.math3.random.RandomDataGenerator;

import java.util.WeakHashMap;

public class NormalSampler extends AbstractRealSampler implements Cloneable {

	private final  double mean;
	private final double sd;

//	private NormalSampler() {
//		super();
//	}

	private NormalSampler(double mean, double sd) {
		super();
		this.mean = mean;
		this.sd = sd;
	}

	private static final WeakHashMap<Integer, NormalSampler> CACHE = new WeakHashMap<>();

	public static NormalSampler create(double mean, double sd)
	{
		int hash = hash(mean, sd);
		synchronized (CACHE)
		{
			return CACHE.computeIfAbsent(hash, i -> new NormalSampler(mean, sd));
		}
	}

	public static int cacheSize()
	{
		synchronized (CACHE)
		{
			return CACHE.size();
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NormalSampler that = (NormalSampler) o;

		if (Double.compare(that.mean, mean) != 0) return false;
		return Double.compare(that.sd, sd) == 0;

	}

	private static int hash(double mean, double sd)
	{
		int result;
		long temp;
		temp = Double.doubleToLongBits(mean);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(sd);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public int hashCode()
	{
		return hash(mean, sd);
	}

	public void set(double mean, double sd) {
//		this.mean = mean;
//		this.sd = sd;
		throw new UnsupportedOperationException();
	}

	public void setMean(double mean) {
//		this.mean = mean;
		throw new UnsupportedOperationException();
	}

//	public void setSd(double sd) {
//		this.sd = sd;
//	}

	public double getMean() {
		return mean;
	}

	public double getSd() {
		return sd;
	}

	@Override
	public double next(RandomDataGenerator rdg) {
		return rdg.nextGaussian(mean, sd);
	}

	@Override
	public void setLower(double lower) {
		// do nothing.

	}

	@Override
	public void setUpper(double upper) {
		// do nothing.

	}

	@Override
	public NormalSampler clone()
	{
//		try
//		{
//		NormalSampler retval = (NormalSampler) super.clone();
		return NormalSampler.create(mean, sd);
//		} catch (CloneNotSupportedException e)
//		{
//			e.printStackTrace();
//			throw new RuntimeException(e); // This should not happen.
//		}
	}
}
