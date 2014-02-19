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
	
	private final String ModuleName = "Network";
	
	/**
	 * Constructor
	 * @param logPath The location of the logFile
	 */
	public Log (String logPath, Boolean debugMode)
	{
		DateFormat yearMoth = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		this.logFilePath = logPath + yearMoth.format(date) + "-" + ModuleName + ".log";
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
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS"); 
			Date date = new Date();
			
			File logFile = new File(logFilePath);
			RandomAccessFile rLogFile = new RandomAccessFile(logFile, "rw");
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
