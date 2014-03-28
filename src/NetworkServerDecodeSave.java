import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.io.File;

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
	Semaphore databaseAccess;
	
	
	//Database info
	String user = "root";
	String password = "abc88";
	String URL = "//localhost/munin";
	
	
	public NetworkServerDecodeSave (Log log, String imageFileSavePath, SynchronousQueue<String> queue, Semaphore databaseAccess)
	{
		this.log = log;
		this.imageFileSavePath = imageFileSavePath;
		this.queue = queue;
		this.databaseAccess = databaseAccess;
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
				
				new File(xmlFilePath).delete();
				log.print("Deleted old files");
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
		String imageName =  xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 1).split("\\.")[0] + ".rgb"; //Adds file name and image typ
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
				
				
				////////////////////////////////////////////
				//Different kinds of checks
				////////////////////////////////////////////
				
				//Inserts new course if needed
				Statement containsState = connect.createStatement();
				ResultSet containsResult =  containsState.executeQuery("SELECT code " +
																		"FROM courses " +
																		"WHERE code='" + 
																		xmlEditor.readCourseCode() + "'");
				if (!containsResult.next())
				{
					Statement insertStat = connect.createStatement();
					insertStat.executeUpdate("INSERT INTO courses(name, period, code) " +
												"VALUES('" + xmlEditor.readLectureName() + 
												"', '" + period + "', '" + 
												xmlEditor.readCourseCode() + "')");
					insertStat.close();
				}	
				
				//Checks if camera_unit exits, if not wait. Database should always have the camera unit
				Boolean hasCameraUnit = false;
				while (!hasCameraUnit)
				{
					containsResult = containsState.executeQuery("SELECT name " +
																"FROM camera_units " +
																"WHERE name='" + 
																xmlEditor.readRasPiId() + "'");
					if (containsResult.next())
					{
						hasCameraUnit = true;
					}
					sleep(30000); //30sec
				}
				
				containsState.close();
				containsResult.close();
				
				
				////////////////////////////////////////////
				//Inserts incoming lecture note into database
				////////////////////////////////////////////
				
				//Creates statement that will be used later
				Statement lectureNoteState =  connect.createStatement();
				Statement lectureGetState =  connect.createStatement();

				
				//Gets matching lecture
				databaseAccess.acquire();
				ResultSet lectureResult = lectureGetState.executeQuery("SELECT id, startTime, endTime " +
																		"FROM lectures " +
																		"WHERE course_code='" + xmlEditor.readCourseCode() + "' " +
																		"AND course_period='" + period + "' " +
																		"AND startTime='" + xmlEditor.readLectureTime().split("\\;")[0] + "'");
				
				if (lectureResult.next())
				{
					//Creates new lecture note
					databaseAccess.release();
					lectureNoteState.executeUpdate("INSERT INTO lecture_notes(id, lecture_id, " +
													"camera_unit_name, processed, time, image) " +
													"VALUES (null, '" + lectureResult.getString(1) + 
													"', '" + imageName.split("\\_")[0] + "', 0, '" + 
													imageTimeWithSec + "', '" + imageFileSavePath + 
													subPath + imageName.split("\\.")[0] + ".png')");
					log.print("Found lecture, inserting new lecture note to that lecture");
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted new " +
							"lecture note into lecture with id: " + lectureResult.getString(1));
					
				}
				else
				{
					//Creating new lecture
					Statement lectureState = connect.createStatement();
					lectureState.executeUpdate("INSERT INTO lectures(id, course_code, course_period, " +
											"lecture_hall_name, startTime, endTime, finished) " +
											"VALUES (null, '" + xmlEditor.readCourseCode() + "', '" + 
											period + "', '" + xmlEditor.readLectureHall() + 
											"', '" + xmlEditor.readLectureTime().split("\\;")[0] + 
											"', '" + xmlEditor.readLectureTime().split("\\;")[1] + 
											"', 0)");
					databaseAccess.release();
					
					//Finding the lecture that was just created. Need to know what id it got.
					lectureGetState = connect.createStatement();
					lectureResult = lectureGetState.executeQuery("SELECT id " +
																	"FROM lectures " +
																	"WHERE course_code='" + xmlEditor.readCourseCode() 
																	+ "' " + "AND course_period='" + period + "' " +
																	"AND startTime='" + xmlEditor.readLectureTime().split("\\;")[0] + "'");
					
					//Creates new lecture note and insert it with the newly created lecture
					lectureResult.next();
					lectureNoteState.executeUpdate("INSERT INTO lecture_notes(id, lecture_id, " +
													"camera_unit_name, processed, time, image) " +
													"VALUES (null, '" + lectureResult.getString(1) + 
													"', '" + imageName.split("\\_")[0] + "', 0, '" + 
													imageTimeWithSec + "', '" + imageFileSavePath + 
													subPath + imageName.split("\\.")[0] + ".png')");
					
					log.print("Created new lecture that got id: " + lectureResult.getString(1) + 
														" and inserted a lecture note into it");
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Created new " +
											"lecture with id: " + lectureResult.getString(1) + 
											" and inserted a lecture not into it");
					lectureState.close();					
				}
				lectureNoteState.close();
				lectureResult.close();
				lectureGetState.close();
				
			}
			catch (SQLException e)
			{
				log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			}
			catch (ClassNotFoundException e) 
			{
				log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			} 
			catch (InterruptedException e) 
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
			ResultSet containsResult;
			
			//Inserts new lecture hall if needed
			containsResult =  containsState.executeQuery("SELECT name FROM lecture_halls WHERE name='" + 
																		xmlEditor.readLectureHall() + "'");
			if (!containsResult.next())
			{
				Statement insertState = connect.createStatement();
				insertState.executeUpdate("INSERT INTO lecture_halls(name) " +
											"VALUES('" +  xmlEditor.readLectureHall() + "')");
				insertState.close();
			}
			
			//Finds a camera unit with the correct name
			containsResult = containsState.executeQuery("SELECT name FROM camera_units WHERE name='" +
																		xmlEditor.readRasPiId() + "'");			
			Statement updateState = connect.createStatement();
			if (containsResult.next())
			{
				//Updates camera_unit
				int updresult = updateState.executeUpdate("UPDATE camera_units " +
															"SET lecture_hall_name='" + xmlEditor.readLectureHall() + 
															"', ip_address='" + xmlEditor.readRasPiIpAddress() + 
															"', password='" + xmlEditor.readRasPiPassword() + "' " +
															"WHERE name='" + xmlEditor.readRasPiId() + "'");
				if (updresult == 1)
				{
					log.print("Updated database with new configs for: " + xmlEditor.readRasPiId());
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Updated database with " +
														"new configs for: " + xmlEditor.readRasPiId());
				}
			}
			else
			{
				//Insert new camera_unit
				int insertResult = updateState.executeUpdate("INSERT INTO camera_units(name, lecture_hall_name, " +
																						"ip_address, password) " +
															"VALUES('" + xmlEditor.readRasPiId() + "', '" + 
																	xmlEditor.readLectureHall() + "', '" + 
																	xmlEditor.readRasPiIpAddress() + "', '" + 
																	xmlEditor.readRasPiPassword() +"')");
				if (insertResult == 1)
				{
					log.print("Inserted a new camera unit into database with id: " + xmlEditor.readRasPiId());
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted new config " +
									"for a Rasberry Pi in database with id: " + xmlEditor.readRasPiId());
				}
			}
			updateState.close();
			containsState.close();
			containsResult.close();
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
