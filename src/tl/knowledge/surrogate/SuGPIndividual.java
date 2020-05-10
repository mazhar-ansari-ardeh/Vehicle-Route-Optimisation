package tl.knowledge.surrogate;

import tl.gp.TLGPIndividual;

public class SuGPIndividual extends TLGPIndividual
{
	public double getSurFit()
	{
		return surFit;
	}

	public void setSurFit(double surFit)
	{
		this.surFit = surFit;
	}

	public long getSurtime()
	{
		return surtime;
	}

	public void setSurtime(long surtime)
	{
		this.surtime = surtime;
	}

//	public long getEvalTime()
//	{
//		return evalTime;
//	}
//
//	public void setEvalTime(long evalTime)
//	{
//		this.evalTime = evalTime;
//	}

	transient private double surFit;
	transient private long surtime;
//	transient private long evalTime;
}
