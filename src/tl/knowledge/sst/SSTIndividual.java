package tl.knowledge.sst;

import tl.gp.TLGPIndividual;

public class SSTIndividual extends TLGPIndividual
{
	public void setOrigin(IndividualOrigin origin)
	{
		this.origin = origin.toString();
	}
}
