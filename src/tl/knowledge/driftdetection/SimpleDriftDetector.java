package tl.knowledge.driftdetection;

public class SimpleDriftDetector extends BaseDriftDetector
{
	private Double previousValue = null;

	/**
	 * Minimum number of instances to ignore before drift detection is activated. Using this allows
	 * to ignore initial steps and generations of an algorithm to allow it to reach into some state
	 * of maturity.
	 */
	private int minNumberOfInstances;

	/**
	 * Number of instances that has been fed into this detector.
	 */
	private int instances = 0;

	public SimpleDriftDetector(int minNumberOfInstances)
	{
		this.minNumberOfInstances = minNumberOfInstances;
	}

	@Override
	public void add_element(double input_value)
	{
		if(previousValue == null)
		{
			previousValue = input_value;
		}

		in_concept_change = (instances > minNumberOfInstances) && (previousValue < input_value);
		previousValue = input_value;
		instances++;
	}

	@Override
	public void reset()
	{
		super.reset();
		previousValue = null;
		instances = 0;
	}
}
