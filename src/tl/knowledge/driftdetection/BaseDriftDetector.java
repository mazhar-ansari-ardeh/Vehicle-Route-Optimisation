package tl.knowledge.driftdetection;

/**
 * Abstract Drift Detector

    Any drift detector class should follow this minimum structure in
    order to allow interchangeability between all change detection
    methods.

    NotImplementedError. All child classes should implement the
    get_info function.

    This is a port of the library at:
    {@link https://github.com/scikit-multiflow/scikit-multiflow/tree/master/src/skmultiflow/drift_detection}

 * @author mazhar
 */
public abstract class BaseDriftDetector
{

	protected boolean in_concept_change;
	protected boolean in_warning_zone;
	protected double estimation;
	protected double delay;

	public BaseDriftDetector()
	{
		in_concept_change = false;
		in_warning_zone = false;
		estimation = 0;
		delay = 0;
	}

	/**
	 * Resets the change detector parameters.
	 */
	public void reset()
	{
		in_concept_change = false;
		in_warning_zone = false;
		estimation = 0.0;
		delay = 0.0;
	}

	/**
	 * This function returns whether concept drift was detected or not.
	 * @return Whether concept drift was detected or not.
	 */
	public boolean detected_change()
	{
		return in_concept_change;
	}

	/**
	 * If the change detector supports the warning zone, this function will return
	 * whether it's inside the warning zone or not.
	 * @return Whether the change detector is in the warning zone or not.
	 */
	public boolean detected_warning_zone()
	{
        return in_warning_zone;
	}

    /**
     * Returns the length estimation.
     * @return The length estimation
     */
    public double get_length_estimation()
    {
        return estimation;
    }

    /**
     * Adds the relevant data from a sample into the change detector.
     * @param input_value: Whatever input value the change detector takes.
     * @returns: BaseDriftDetector self, optional
     */
    public abstract void add_element(double input_value);
}
