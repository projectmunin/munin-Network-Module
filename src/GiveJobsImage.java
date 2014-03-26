import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

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
	
	/**
	 * Gives note jobs to image analysis program
	 * @return True if found a job otherwise false
	 */
	private static boolean giveJob ()
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager
						.getConnection("jdbc:mysql:" + URL + "?user="+ user + "&password=" + password);
				
			//Gets all the current unprocessed notes 
			Statement getUnStatement = connect.createStatement();
			ResultSet unprocessedNotes = getUnStatement.executeQuery("SELECT id, lecture_id, image " +
				  													"FROM lecture_notes " +
				  													"WHERE processed = '0' " +
				  													"ORDER BY time;");
			//Starts to send jobs to analysis program
			Boolean foundUnprocessedNotes = false;
			log.print("Starts to send jobs to analysis prog if their is any");
			while (unprocessedNotes.next())
			{
				//Get all previous notes for that lecture that have already been processed
				
				Statement getPrevStatement = connect.createStatement();
				ResultSet previousNotes = getPrevStatement.executeQuery("SELECT image " +
																		"FROM lecture_notes " +
																		"WHERE lecture_id = '" + 
																		unprocessedNotes.getString(2) + 
																		"' AND id != '" + 
																		unprocessedNotes.getString(1) +
																		"' AND processed = '1'");
				//Gets all the images to be sent to analysis program
				String imagePaths = "";
				while (previousNotes.next())
				{
					imagePaths = imagePaths + " " + previousNotes.getString(1);
				}
				getPrevStatement.close();
				previousNotes.close();
				log.print("Found processed files: " + imagePaths + " from lecture with id: " + 
																unprocessedNotes.getString(2));
				
				//Start analysis program
				if (!imagePaths.equals(""))
				{
					Process externProgram;
					externProgram = Runtime.getRuntime().exec(imageAnalysProgPath + " -n" + 
											unprocessedNotes.getString(3) + "-i" + imagePaths);
					
					//Updates the unprocessed note to processed
					if (externProgram.waitFor() == 0)
					{
						log.print("The image analys program succeeded to analys image with id: " + 
																	unprocessedNotes.getString(1));
						log.write(true, "[SUCCESS] GiveJobsImage; The image analys program " +
								"succeeded to analys image with id: " + unprocessedNotes.getString(1));
						
						Statement  updateStatement = connect.createStatement();
						int result = updateStatement.executeUpdate("UPDATE lecture_notes " +
																	"SET processed=1 " +
																	"WHERE id='" + 
																	unprocessedNotes.getString(1) + "'");
						if (result == 1)
						{
							log.write(true, "[SUCCESS] GiveJobsImage; Updated note with id: " 
													+ unprocessedNotes.getString(1) + " to processed");
						}
						updateStatement.close();
						 
					}
					//Deletes the unprocessed note because of a match
					else if (externProgram.exitValue() == 2)
					{
						log.print("The image analys program succeeded to analys image with id: " + 
																	unprocessedNotes.getString(1));
						log.write(true, "[SUCCESS] GiveJobsImage; The image analys program " +
							"succeeded to analys image with id: " + unprocessedNotes.getString(1));
						
						Statement  updateStatement = connect.createStatement();
						int result = updateStatement.executeUpdate("DELETE FROM lecture_notes " +
																	"WHERE id='" + 
																	unprocessedNotes.getString(1) + "'");
						
						if (result == 1)
						{
							log.write(true, "[SUCCESS] GiveJobsImage; Deleted note with id: " 
																+ unprocessedNotes.getString(1));
						}
						updateStatement.close();
					}
					else
					{
						log.write(false, "[ERROR] GiveJobsImage; Image analys encountered a " +
								"problem analysing note with id: " + unprocessedNotes.getString(1) + 
								" analysis program returned error number: " + externProgram.exitValue());
					}
				}
				//If first note, makes it processed
				else
				{
					Statement  updateStatement = connect.createStatement();
					int result = updateStatement.executeUpdate("UPDATE lecture_notes " +
																"SET processed=1 " +
																"WHERE id='" + 
																unprocessedNotes.getString(1) + "'");
					if (result == 1)
					{
						log.write(true, "[SUCCESS] GiveJobsImage; Updated note with id: " 
												+ unprocessedNotes.getString(1) + " to processed");
					}
					updateStatement.close();
				}
				foundUnprocessedNotes = true;
			}
			getUnStatement.close();
			unprocessedNotes.close();
			return foundUnprocessedNotes;			
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
