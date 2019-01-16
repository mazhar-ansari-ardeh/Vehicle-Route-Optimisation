package tl.knowledge.driftdetection;

/**
 * Page Hinkley change detector
 * This change detection method works by computing the observed values and their mean up to the
 * current moment. Page Hinkley won't output warning zone warnings, only change detections. The
 * method works by means of the Page Hinkley test. In general lines it will detect a concept drift
 * if the observed mean at some instant is greater then a threshold value lambda.
 *
 *  Examples
 *   --------
 *  PageHinkley ph = new PageHinkley();
 *  // Simulating a data stream as a normal distribution of 1's and 0's
 *  data_stream = np.random.randint(2, size=2000)
 *  // Changing the data concept from index 999 to 2000
 *  for(int i = 999; i < 2000, i++)
 *      data_stream[i] = Math.nextInt(8)
 *  // Adding stream elements to the PageHinkley drift detector and verifying if drift occurred
 *  for(int i = 0; i < 2000; i++):
 *      ph.add_element(data_stream[i])
 *      if(ph.detected_change())
 *          System.out.println("Change has been detected in data: " + data_stream[i] + " - of index: " + i);
 *
 * @author mazhar
 */
public class PageHinkley extends BaseDriftDetector
{
	private int min_instances;
	private double delta;
	private double _lambda;
	private double alpha;
	private double x_mean;
	private double sample_count;
	private double sum;

	public PageHinkley()
	{
		this(30, 0.005, 50, 1 - 0.0001);
	}

	public PageHinkley(int numInstances)
	{
		this(numInstances, 0.005, 50, 1 - 0.0001);
	}

	/**
	 * Create a new Page-Hinkley detector.
	 * @param min_num_instances The minimum number of instances before detecting change.
	 * @param delta The delta factor for the Page Hinkley test.
	 * @param _lambda The change detection threshold.
	 * @param alpha The forgetting factor, used to weight the observed value and the mean.
	 */
	public PageHinkley(int min_num_instances, double delta, double lambda, double alpha)
	{
		super();
        min_instances = min_num_instances;
        this.delta = delta;
        this._lambda = lambda;
        this.alpha = alpha;
        x_mean = 0;
        sample_count = 0;
        sum = 0;
        reset();
	}


	/**
	 * Add a new element to the statistics<p>
	 *
	 * <b>Note</b>: After calling this method, to verify if change was detected, one should call the
	 * super method {@code detected_change}, which returns {@code true }if concept drift was
	 * detected and {@code false} otherwise.
	 *
	 * @param x The observed value, from which we want to detect the concept change.
	 */
	@Override
	public void add_element(double x)
	{
		if (in_concept_change)
            reset();

        x_mean = x_mean + (x - x_mean) / 1.0 * sample_count;
        sum = alpha * sum + (x - x_mean - delta);

        sample_count += 1;

        estimation = x_mean;
        in_concept_change = false;
        in_warning_zone = false;

        delay = 0;

        if (sample_count < min_instances)
            return;

        if (sum > _lambda)
            in_concept_change = true;
	}

	/**
	 * Resets the change detector parameters.
	 */
	@Override
	public void reset()
	{
		reset();
        this.sample_count = 1;
        this.x_mean = 0.0;
        this.sum = 0.0;
	}
}
