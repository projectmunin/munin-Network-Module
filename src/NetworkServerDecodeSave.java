import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.SynchronousQueue;

/**
 * Decodes incoming xmlfile, save the image at correct place and inserts data into data base
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
	String password = "abc88"; //TODO rm
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
		String period = getPeriod(imageTime);
		
		if (period.equals(""))
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; Invalid image. Image wasn't taken in a period");
		}
		else
		{
			//Creates path and save image
			String subPath = xmlEditor.readCourseCode() + "/" + period + "/" + imageTime.replace("-", "/") + "/";
			xmlEditor.decodeImage(imageFileSavePath + subPath + imageName); 
			log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Stored image at: " + 
														imageFileSavePath + subPath + imageName);
			
			//Insert data into database
			//TODO insert into database maybe create a view/trigger that when you insert a new 
			//lecture note. creates auto a new lecutre if needed and new course of not exesting or something like it 
		}
	}
	
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
			if (!containResult.next())
			{
				Statement insertStat = connect.createStatement();
				int insertResult = insertStat.executeUpdate("INSERT INTO camera_units(name) " +
																"VALUES('" +  xmlEditor.readLectureHall() + "')");
				if (insertResult == 1)
				{
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted updated " +
											"or new config for Rasberry Pi: " + xmlEditor.readRasPiId());
				}

			}
			
			//Updates rasPi if exits otherwise insert new
			containResult = containsState.executeQuery("SELECT name FROM camera_units WHERE name='" +
																		xmlEditor.readRasPiId() + "'");			
			Statement updateState = connect.createStatement();
			if (containResult.next())
			{
				//Updates camera_unit
				int updresult = updateState.executeUpdate("UPDATE camera_units " +
															"SET lecture_hall_name='" + xmlEditor.readLectureHall() + 
															"', ip_address='" + xmlEditor.readRasPiIpAddress() + 
															"', password='" + xmlEditor.readRasPiPassword() + "' " +
															"WHERE name='" + xmlEditor.readRasPiId() + "'");
				if (updresult == 1)
				{
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted updated " +
											"or new config for Rasberry Pi: " + xmlEditor.readRasPiId());
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
					log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Inserted updated " +
											"or new config for Rasberry Pi: " + xmlEditor.readRasPiId());
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
