import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.SynchronousQueue;

/**
 * Decodes incoming xmlfile. Save the image at correct place and 
 * inserts data into data base if it was a image xml. If it was
 * a Raspberry Pi new config file. Updates the database for that
 * Raspberry Pi
 * 
 * @author P. Andersson
 *
 */
public class NetworkServerDecodeSave extends Thread implements Runnable 
{
	//Private class variables
	Log log;
	SynchronousQueue<String> queue;
	String imageFileSavePath;
	int intervalBetweenTries = 500; //0.5seconds
	
	//Database info
	String user = "root";
	String password = "abc88";
	String URL = "//localhost/munin";
	
	
	public NetworkServerDecodeSave (Log log, String imageFileSavePath, SynchronousQueue<String> queue)
	{
		this.log = log;
		this.imageFileSavePath = imageFileSavePath;
		this.queue = queue;
	}
	
	public void run ()
	{
		try
		{
			while (true)
			{
				String xmlFilePath = queue.take();
				log.print("Received this xmlfile: \"" + xmlFilePath + "\" from NetworkServer");
				
				while(!isCompletelyWritten(xmlFilePath))
				{
					sleep(intervalBetweenTries);
				}
				
				if (xmlFilePath.contains("config_") && xmlFilePath.contains(".xml"))
				{
					log.print("Starting to update raspi config in the database");
					updateDataBaseRasPiConfig(xmlFilePath);
					log.print("Done updating raspi config in the database");
				}
				else if (xmlFilePath.contains(".xml"))
				{
					log.print("Starting to decode xmlfile");
					decodeXmlFile(xmlFilePath); //Should decode, save image at correct position and insert data in database.
					log.print("Decoded image from xmlfile, saved image at correct " + 
												"position and inserted data in database");
				}
				else
				{
					log.write(false, "[ERROR] Network-NetworkServerDecodeSave; Strange file was " +
															"located in scan folder: " + xmlFilePath);
				}
				
//				new File(xmlFilePath).delete();
//				log.print("Deleted old files");
			}			
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
		}
	}
	
	/**
	 * Decode incoming xmlfile. Stores the image at correct position and inserts 
	 * relevant data into the database
	 * @param xmlFilePath The incoming xmlfile to be decoded
	 */
	private void decodeXmlFile (String xmlFilePath)
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(xmlFilePath, log);
		String imageName =  xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 1).split("\\.")[0] + ".png"; //Adds file name and image typ
		String imageTime = imageName.split("\\_")[1];
		String imageTimeWithSec = imageTime + "_" + imageName.split("\\_")[2];
		String period = getPeriod(imageTime);
		
		if (period.equals(""))
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; Invalid image. Image " +
					"					wasn't taken in a period. Path to file: " + xmlFilePath);
		}
		else
		{
			//Creates path and save image
			String subPath = xmlEditor.readCourseCode() + "/" + period + "/" + imageTime.replace("-", "/") + "/";
			xmlEditor.decodeImage(imageFileSavePath + subPath + imageName); 
			log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Stored image at: " + 
														imageFileSavePath + subPath + imageName);
			
			try
			{				
				//Connects to database
				Class.forName("com.mysql.jdbc.Driver");
				Connection connect = DriverManager
						.getConnection("jdbc:mysql:" + URL + "?user="+ user + "&password=" + password);
				
				Statement containsState = connect.createStatement();
				ResultSet containResult;
				
				
				//Inserts new course if needed
				containResult =  containsState.executeQuery("SELECT code FROM courses WHERE code='" + 
																		xmlEditor.readCourseCode() + "'");
				if (!containResult.next())
				{
					Statement insertStat = connect.createStatement();
					insertStat.executeUpdate("INSERT INTO courses(name, period, code) " +
												"VALUES('" + xmlEditor.readLectureName() + 
												"', '" + period + "', '" + 
												xmlEditor.readCourseCode() + "')");
					insertStat.close();
				}		
				
				
				//Creates statement that will be used later
				Statement lectureNoteState =  connect.createStatement();
				Statement lectureGetState =  connect.createStatement();
				
				//Finds all lectures for a course with the correct lecture hall and period
				ResultSet lectureResult = lectureGetState.executeQuery("SELECT id, startTime, endTime " +
																	"FROM lectures " +
																	"WHERE course_code='" + xmlEditor.readCourseCode() + "' " +
																	"AND course_period='" + period + "' " +
																	"AND lecture_hall_name='" + xmlEditor.readLectureHall() + "'");


				Boolean lectureFound = false;
				while (!lectureFound && lectureResult.next())
				{
					lectureFound = isLectureNoteInLecture(imageTimeWithSec, lectureResult.getString(2), lectureResult.getString(3));
				}
				
				if (lectureFound)
				{
					//Creates new lecture note
					System.out.println("found"); //TODO rm
					lectureNoteState.executeUpdate("INSERT INTO lecture_notes(id, lecture_id, " +
													"camera_unit_name, processed, time, image) " +
													"VALUES (null, '" + lectureResult.getString(1) + 
													"', '" + imageName.split("\\_")[0] + "', 0, '" + 
													imageTimeWithSec + "', '" + imageFileSavePath + 
													subPath + imageName + "')");
					log.print("Found lecture, inserting new lecture note to that lecture");
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted new " +
							"lecture note into lecture with id: " + lectureResult.getString(1));
					lectureNoteState.close();
					lectureResult.close();
				}
				else
				{
					//Creating new lecture
					Statement lectureState = connect.createStatement();
					lectureState.execute("INSERT INTO lectures(id, course_code, course_period, " +
											"lecture_hall_name, startTime, endTime, finished) " +
											"VALUES (null, '" + xmlEditor.readCourseCode() + "', '" + 
											period + "', '" + xmlEditor.readLectureHall() + 
											"', '2012-06-06 10:00:00', '2012-06-06 11.45.00', 0)"); //TODO fix the time
					
					//Finding the lecture that was just created. Need to know what id it got.
					lectureGetState = connect.createStatement();
					lectureResult = lectureGetState.executeQuery("SELECT id " +
																	"FROM lectures " +
																	"WHERE course_code='" + xmlEditor.readCourseCode() 
																	+ "' " + "AND course_period='" + period + "' " +
																	"AND startTime='2012-06-06 10:00:00'"); //TODO fix the time
					
					//Creates new lecture note
					lectureResult.next();
					lectureNoteState.executeUpdate("INSERT INTO lecture_notes(id, lecture_id, " +
													"camera_unit_name, processed, time, image) " +
													"VALUES (null, '" + lectureResult.getString(1) + 
													"', '" + imageName.split("\\_")[0] + "', 0, '" + 
													imageTimeWithSec + "', '" + imageFileSavePath + 
													subPath + imageName + "')");
					
					log.print("Created new lecture that got id: " + lectureResult.getString(1) + 
														" and inserted a lecture note into it");
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Created new " +
											"lecture with id: " + lectureResult.getString(1) + 
											" and inserted a lecture not into it");
					lectureState.close();
					lectureGetState.close();
					lectureResult.close();
					lectureNoteState.close();
				}
				
			}
			catch (SQLException e)
			{
				log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			}
			catch (ClassNotFoundException e) 
			{
				log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			}
		}
	}
	
	/**
	 * Updates the database with new Raspberry Pi configs
	 * @param xmlFilePath The new Raspberry Pi xmlconfig file
	 */
	private void updateDataBaseRasPiConfig (String xmlFilePath)
	{
		try
		{
			EncodeDecodeXml xmlEditor = new EncodeDecodeXml(xmlFilePath, log);
			
			//Connects to database
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager
					.getConnection("jdbc:mysql:" + URL + "?user="+ user + "&password=" + password);
			
			
			Statement containsState = connect.createStatement();
			ResultSet containResult;
			
			//Inserts new lecture hall if needed
			containResult =  containsState.executeQuery("SELECT name FROM lecture_halls WHERE name='" + 
																		xmlEditor.readLectureHall() + "'");
			System.out.println("check");
			if (!containResult.next())
			{
				System.out.println("found");
				System.out.println(containResult.getString(1) + " found string");
				Statement insertStat = connect.createStatement();
				insertStat.executeUpdate("INSERT INTO lecture_halls(name) " +
											"VALUES('" +  xmlEditor.readLectureHall() + "')");
			}
			
			//Finds a camera unit with the correct name
			containResult = containsState.executeQuery("SELECT name FROM camera_units WHERE name='" +
																		xmlEditor.readRasPiId() + "'");			
			Statement updateState = connect.createStatement();
			if (containResult.next())
			{
				//Updates camera_unit
				System.out.println("upt");
				int updresult = updateState.executeUpdate("UPDATE camera_units " +
															"SET lecture_hall_name='" + xmlEditor.readLectureHall() + 
															"', ip_address='" + xmlEditor.readRasPiIpAddress() + 
															"', password='" + xmlEditor.readRasPiPassword() + "' " +
															"WHERE name='" + xmlEditor.readRasPiId() + "'");
				if (updresult == 1)
				{
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Updated database with " +
														"new Rasberry Pi for: " + xmlEditor.readRasPiId());
				}
			}
			else
			{
				//Insert new camera_unit
				System.out.println("ins");
				int insertResult = updateState.executeUpdate("INSERT INTO camera_units(name, lecture_hall_name, " +
																						"ip_address, password) " +
															"VALUES('" + xmlEditor.readRasPiId() + "', '" + 
																	xmlEditor.readLectureHall() + "', '" + 
																	xmlEditor.readRasPiIpAddress() + "', '" + 
																	xmlEditor.readRasPiPassword() +"')");
				if (insertResult == 1)
				{
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted new config " +
									"for a Rasberry Pi in database with id: " + xmlEditor.readRasPiId());
				}
			}
		}
		catch (SQLException e)
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
		}
		catch (ClassNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
		}
	}
	
	/**
	 * Checks if lecture note in a lecture
	 * @param note The note time, Syntax "yyyy-mm-dd_HH-MM-ss" Doesn't matters if it's - or : 
	 * between times or anything else. Only need to be any character between the times.
	 * @param startTime The start time of the lecture, Syntax "yyyy-mm-dd HH:MM:ss"
	 * @param endTime The end time of a lecture, Syntax "yyyy-mm-dd HH:MM:ss"
	 * @return True if note is between start and endtime+14.99min 
	 */
	private boolean isLectureNoteInLecture(String note, String startTime, String endTime)
	{
		try
		{
			Calendar noteCalendar = Calendar.getInstance();
			noteCalendar.set(Calendar.YEAR, Integer.parseInt(note.substring(0, 4)));
			noteCalendar.set(Calendar.MONTH, Integer.parseInt(note.substring(5, 7)) - 1);
			noteCalendar.set(Calendar.DATE, Integer.parseInt(note.substring(8, 10)));
			noteCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(note.substring(11, 13)));
			noteCalendar.set(Calendar.MINUTE, Integer.parseInt(note.substring(14, 16)));
			noteCalendar.set(Calendar.SECOND, Integer.parseInt(note.substring(17, 19)));	
			
			
			Calendar startCalendar = Calendar.getInstance();
			startCalendar.set(Calendar.YEAR, Integer.parseInt(startTime.substring(0, 4)));
			startCalendar.set(Calendar.MONTH, Integer.parseInt(startTime.substring(5, 7)) - 1);
			startCalendar.set(Calendar.DATE, Integer.parseInt(startTime.substring(8, 10)));
			startCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime.substring(11, 13)));
			startCalendar.set(Calendar.MINUTE, Integer.parseInt(startTime.substring(14, 16)));
			startCalendar.set(Calendar.SECOND, Integer.parseInt(startTime.substring(17, 19)) - 1);	//Extends start time with 1 second, because a note can be at exaxtly the start time
			
			
			Calendar endCalendar = Calendar.getInstance();
			endCalendar.set(Calendar.YEAR, Integer.parseInt(endTime.substring(0, 4)));
			endCalendar.set(Calendar.MONTH, Integer.parseInt(endTime.substring(5, 7)) - 1);
			endCalendar.set(Calendar.DATE, Integer.parseInt(endTime.substring(8, 10)));
			endCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime.substring(11, 13)));
			endCalendar.set(Calendar.MINUTE, Integer.parseInt(endTime.substring(14, 16)) + 14); //Extends the end of lecture to 14.99min after lecture
			endCalendar.set(Calendar.SECOND, Integer.parseInt(endTime.substring(17, 19)) + 59);	

			return (noteCalendar.after(startCalendar) && noteCalendar.before(endCalendar));
		}
		catch(NumberFormatException e)
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Checks if a file has been completely written. Uses the linux program lsof. ONLY works for linux
	 * @param file The file that will be checked if it has been written completely
	 * @return true if it has been completely written, otherwise false;
	 */
	private boolean isCompletelyWritten (String filePath)
	{
		try 
		{
			Process plsof = new ProcessBuilder(new String[]{"lsof", "|", "grep", filePath}).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
			if (reader.readLine() == null)
			{
				plsof.destroy();
				reader.close();
				return true;
			}
			plsof.destroy();
			reader.close();
			return false;
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			return false;
		}
	}	
	
	/**
	 * Returns the period for the input image taken time. If period changes, you can see
	 * how is looks in this link:
	 *		 https://www.student.chalmers.se/sp/academic_year_list
	 * @param imageTime The time the image been taken. Inputs layout "yyyy-MM-dd"
	 * @return What period the image was taken. Returns empty string if no period was found
	 */
	private String getPeriod (String imageTime)
	{
		int currentMonth = Integer.parseInt(imageTime.substring(5, 7));
		int currentDay = Integer.parseInt(imageTime.substring(8, 10));
		int currentYear = Integer.parseInt(imageTime.substring(2, 4));
		
		if (9 <= currentMonth && 10 >= currentMonth) //First period in a school year
		{
			return "HT" + currentYear + "-1";
		}
		else if (11 == currentMonth || 12 == currentMonth || (1 == currentMonth && currentDay <= 9)) //Second period
		{
			return "HT" + currentYear + "-2";
		}
		else if (1 <= currentMonth && (3 >= currentMonth && currentDay <= 15)) //Third period
		{
			return "VT" + currentYear + "-3";
		}
		else if (3 <= currentMonth && 6 >= currentMonth) //Fourth period
		{
			return "VT" + currentYear + "-4";
		}
		return "";
	}
}
