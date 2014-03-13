import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;

/**
 * Decodes incoming xmlfile, save the image at correct place and inserts data into data base
 * 
 * @author P. Andersson
 *
 */
public class NetworkServerDecodeSave extends Thread implements Runnable 
{
	//Priate class variables
	Log log;
	SynchronousQueue<String> queue;
	String imageFileSavePath;
	int intervalBetweenTries = 500; //0.5seconds
	
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
				
				log.print("Starting to decode xmlfile");
				decodeXmlFile(xmlFilePath); //Should decode, save image at correct position and insert data in database.
				log.print("Decoded image from xmlfile, saved image at correct " + 
											"position and inserted data in database");
				
//				deleteOldFiles(xmlFilePath);
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
	 *  how is looks in this link:
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
	
	/**
	 * Deletes inputet files
	 * @param xmlFilePath	Path for the xmlfile
	 * @param imageFilePath Path for the imagefile
	 */
	private void deleteOldFiles (String xmlFilePath)
	{
		File xmlFile = new File(xmlFilePath);
		xmlFile.delete();
		log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Deleted old xmlfile: \"" + 
																			xmlFilePath + "\""); 
	}
}
