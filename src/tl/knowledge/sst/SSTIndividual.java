package tl.knowledge.sst;

import ec.Individual;
import tl.gp.TLGPIndividual;

public class SSTIndividual extends TLGPIndividual
{
	private IndividualOrigin origin;

	@Override
	public String getOrigin()
	{
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
