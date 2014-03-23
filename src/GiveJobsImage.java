import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Calendar;


public class GiveJobsImage extends Thread
{
	//Permanent configs
	static Log log;
	static Boolean debugMode = false;
	static int waitTimeBetweenChecks = 180000; //3min
	static int timeWaitBeforeAnalysingLectures = 1; //One hour
	
	static String imageAnalysProgPath = "/root/projectmunin/source/imageProcessing/prog"; //TODO change what the program name is
	
	//Database info
	static String user = "root";
	static String password = "060906";
	static String URL = "//localhost/munin";
	
	public static void main (String[] args) 
	{
		//Reads arguments
		readInput(args);
		
		//Sets folder and Log class
		log = new Log("/home/panda/test/log/", debugMode);  //Log folder, Note: HAS it own log file!
		log = new Log("D:/test/log/", debugMode);
		
		while (true)
		{
			if (!giveJob())
			{
				try 
				{
					sleep(waitTimeBetweenChecks);
				} 
				catch (InterruptedException e) 
				{
					log.write(false, "[ERROR] GiveJobsImage; " + e.getMessage());
				}
			}
		}
	}
	
	private static boolean giveJob ()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager
						.getConnection("jdbc:mysql:" + URL + "?user="+ user + "&password=" + password);
				
			Statement lectureStatement = connect.createStatement();
			ResultSet lecture = lectureStatement.executeQuery("SELECT id, course_code, course_period, time " +
				  												"FROM lectures " +
				  												"WHERE finished = 0 " +
				  												"ORDER BY time;");
			//Starts to send jobs to analysis program
			Boolean foundUnprocessedLectures = false;
			log.print("Starts to send jobs to analysis prog if their is any");
			while (lecture.next())
			{
				if (compareTime(lecture.getString(4)))
				{
					Statement noteStatement = connect.createStatement();;
					ResultSet note = noteStatement.executeQuery("SELECT id, lecture_id, image " +
															"FROM lecture_notes " +
															"WHERE lecture_id = '" + lecture.getString(1) + "'");
					
					//Gets all the images to be sent to analysis program
					String imagePaths = "";
					while (note.next())
					{
						imagePaths = imagePaths + " " + note.getString(3);
					}
					log.print("Found files: " + imagePaths + " for lecture with id: " + lecture.getString(1));
					
					if (!imagePaths.equals(""))
					{
						Process externProgram;
						externProgram = Runtime.getRuntime().exec(imageAnalysProgPath + " -i" + imagePaths);
						
						if (externProgram.waitFor() == 0)
						{
							log.write(true, "[SUCCESS] GiveJobsImage; The image analys program " +
									"succeeded to analys lecture with id: " + lecture.getString(1));
							Statement  updateStatement = connect.createStatement();

							int result = updateStatement.executeUpdate("UPDATE lectures " +
																		"SET finished=1 " +
																		"WHERE id='" + lecture.getString(1) + "'");
							if (result == 1)
							{
								log.write(true, "[SUCCESS] GiveJobsImage; Updated lecture with id: " 
															+ lecture.getString(1) + " to finished");
							}
						}
						else
						{
							log.write(false, "[ERROR] GiveJobsImage; Image analys encountered a " +
									"problem analysing lecture with id: " + lecture.getString(1) + 
									" analysis program returned error number: " + externProgram.exitValue());
						}
					}
					foundUnprocessedLectures = true;
				}
			}
			return foundUnprocessedLectures;
		}
		catch (SQLException e)
		{
			log.write(false, "[ERROR] GiveJobsImage; " + e.getMessage());
		}
		catch (ClassNotFoundException e) 
		{
			log.write(false, "[ERROR] GiveJobsImage; " + e.getMessage());
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] GiveJobsImage; " + e.getMessage());
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] GiveJobsImage; " + e.getMessage());
		}
		return false;
	}

	/**
	 * Compares input time with current time + 1H
	 * @param inputTime The input time. Syntax "2014-03-23 17:15:01"
	 * @return True if current time is after input time (currentTime > inputTime), Otherwise false
	 */
	private static boolean compareTime (String inputTime)
	{
		int inputYear = Integer.parseInt(inputTime.substring(0, 4));
		int inputMonth = Integer.parseInt(inputTime.substring(5, 7)) - 1;
		int inputDay = Integer.parseInt(inputTime.substring(8, 10));
		int inputHour = Integer.parseInt(inputTime.substring(11, 13));
		int inputMin = Integer.parseInt(inputTime.substring(14, 16));
		int inputSec = Integer.parseInt(inputTime.substring(17, 19));

		Calendar lectureTime = Calendar.getInstance();
		lectureTime.set(Calendar.YEAR, inputYear);
		lectureTime.set(Calendar.MONTH, inputMonth);
		lectureTime.set(Calendar.DATE, inputDay);
		lectureTime.set(Calendar.HOUR_OF_DAY, inputHour);
		lectureTime.set(Calendar.MINUTE, inputMin);
		lectureTime.set(Calendar.SECOND, inputSec);	
		
		Calendar currentTimePlus1H = Calendar.getInstance();
		currentTimePlus1H.add(Calendar.HOUR_OF_DAY, timeWaitBeforeAnalysingLectures);

		log.write(true, "[SUCCESS] GiveJobsImage; Imagetime: " + lectureTime.getTime() + 
									" CurrentTimePlus1H: " + currentTimePlus1H.getTime() + 
									" return value: " + currentTimePlus1H.after(lectureTime));
		return currentTimePlus1H.after(lectureTime);
	}
	
	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
		debugMode = false;
		for (int i=0; i < args.length; i++)
		{	
			if (args[i].equals("-h") || args[i].equals("--help"))
			{
				printHelpMessage();
			}
			else if (args[i].equals("-d") || args[i].equals("--debug"))
			{
				debugMode = true;
			}
			else
			{
				System.out.println("ERROR: \"" + args[i] + "\" Unknow command");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Prints the help message and exits
	 */
	private static void printHelpMessage()
	{
		System.out.println("###### GiveJobsImage Help message ######");
		System.out.println(" -d OR --deubg   :Enables debug mode");
		System.out.println(" -h OR --help    :Displays this message\n");
		System.exit(0);
	}
	
}
