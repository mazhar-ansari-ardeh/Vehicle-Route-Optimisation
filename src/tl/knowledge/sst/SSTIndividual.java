package tl.knowledge.sst;

import tl.gp.TLGPIndividual;

public class SSTIndividual extends TLGPIndividual
{
	private IndividualOrigin origin;

	@Override
	public String getOrigin()
	{
		if(origin == null)
			return null;
		return origin.toString();
	}

	public void setOrigin(IndividualOrigin origin)
	{
		this.origin = origin;
	}

	public SSTIndividual lightClone()
	{
		SSTIndividual ind = (SSTIndividual) super.lightClone();
		ind.origin = origin;

		return ind;
	}
}
