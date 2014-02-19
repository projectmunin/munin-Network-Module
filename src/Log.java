import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple log writer that follows syntax with design date and so on.
 * @author P. Andersson
 *
 */
public class Log 
{
	//Private class variables
	private String logFilePath;
	private Boolean debugMode;
	
	/**
	 * Constructor
	 * @param logFilePath The location of the logFile
	 */
	public Log (String logFilePath, Boolean debugMode)
	{
		this.logFilePath = logFilePath;
		this.debugMode = debugMode;
	}
	
	/**
	 * Writes message to file, adds date and other syntax to log file
	 * @param debugMessage Assign to true if it is a debug message otherwise false
	 * @param message The message wanted to write to logfile
	 */
	public void write (Boolean debugMessage, String message)
	{

		try 
		{
			if (!debugMode && debugMessage)
			{
				return;
			}
			File logFile = new File(logFilePath);
			RandomAccessFile rLogFile = new RandomAccessFile(logFile, "rw");
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS"); 
			Date date = new Date();
			rLogFile.seek(logFile.length());
			rLogFile.writeBytes(dateFormat.format(date) + ", " + timeFormat.format(date) + "; " + message + "\n");
			rLogFile.close();
		} 
		catch (IOException e) 
		{
			System.out.println("Class Log: " + e.getMessage());
		}
	}
	
}
