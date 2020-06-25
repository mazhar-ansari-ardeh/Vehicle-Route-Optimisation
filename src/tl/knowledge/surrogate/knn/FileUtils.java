package tl.knowledge.surrogate.knn;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;

public class FileUtils
{
	PrintWriter pw;
	public FileUtils(String fileName)
	{
		this(fileName, true, ".txt");
	}

	public FileUtils(String fileName, boolean timeStamp, String extention)
	{
		if(timeStamp)
		{
			Calendar now = Calendar.getInstance();
//			int year = now.get(Calendar.YEAR);
//			int month = now.get(Calendar.MONTH) + 1; // Note: zero based!
			int day = now.get(Calendar.DAY_OF_MONTH);
			int hour = now.get(Calendar.HOUR_OF_DAY);
			int minute = now.get(Calendar.MINUTE);
			int second = now.get(Calendar.SECOND);
//			int millis = now.get(Calendar.MILLISECOND);
			fileName += String.format("d%d-%d-%d-%d", day, hour, minute, second);
		}
		fileName += extention;
		try
		{
			pw = new PrintWriter(fileName);
		} catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void println(String msg)
	{
		pw.println(msg);
		pw.flush();
	}

	public void println(String... msgs)
	{
		print(msgs);
		pw.println();
	}

	public void print(String... msgs)
	{
		for(String msg : msgs)
			pw.print(msg);
		pw.flush();
	}

	public void close()
	{
		pw.close();
	}
}
