package tl.gp;

import ec.gp.GPIndividual;

class TLGPIndividual extends GPIndividual
{
	private static final long serialVersionUID = 1L;

	private boolean isTested = false;

	private double fitnessOnTrain;

	public boolean isTested() {
		return isTested;
	}

	public void setTested(boolean tested)
	{
		isTested = tested;
	}

	public double getFitnessOnTrain() {
		return fitnessOnTrain;
	}

	public void setFitnessOnTrain(double fitnessOnTrain) {
		this.fitnessOnTrain = fitnessOnTrain;
	}
}

