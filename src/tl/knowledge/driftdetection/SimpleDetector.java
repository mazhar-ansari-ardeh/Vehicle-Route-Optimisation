package tl.knowledge.driftdetection;

public class SimpleDetector extends BaseDriftDetector
{
	private Double previousValue = null;
	private int minNumberOfInstances;
	private int instances = 0;

	public SimpleDetector(int minNumberOfInstances)
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
