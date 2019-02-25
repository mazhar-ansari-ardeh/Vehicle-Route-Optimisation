package tl.gp;

import ec.gp.GPIndividual;

class TLGPIndividual extends GPIndividual
{
	private static final long serialVersionUID = 1L;

	private boolean isTested = false;

	public void setTested(boolean tested)
	{
		isTested = tested;
	}

	public boolean getTested()
	{
		return isTested;
	}
}

