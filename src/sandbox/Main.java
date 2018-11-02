package sandbox;

import java.util.PriorityQueue;

public class Main {

	public static void main(String[] args)
	{
		PriorityQueue<Double> q = new PriorityQueue<>(
				(Double x, Double y)->
				{
					return Double.compare(x, y);
				});

		q.add(3.0);
		q.add(2.0);
		q.add(5.0);
		q.add(1.5);

		int size = q.size();
		for(int i = 0; i < size; i++)
		{
			System.out.println(q.poll());
		}
	}

}
