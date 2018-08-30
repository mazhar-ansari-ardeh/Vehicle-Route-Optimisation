package sandbox;

import java.util.ArrayList;

public class Main {

	public static void main(String[] args)
	{
		ArrayList<String> list = new ArrayList<>();
		list.add("1");
		list.add("2");
		list.add("3");
		list.add("4");

		list.forEach(it -> {System.out.println(it);});
	}

}
