import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Starts the camera controller program in lecture starts. Also notice if new configs arrived
 * from the server.
 * @author P. Andersson
 *
 */
public class NetworkRasPiServerInputs extends Thread implements Runnable 
{
	Log log;
	EncodeDecodeXml currentConfig;
	Semaphore configSem;
	String folderPath;
	SynchronousQueue<String> queue;
	int intervalBetweenTries = 500; //0.5seconds
	
	public NetworkRasPiServerInputs (Log log, String folderPath, EncodeDecodeXml currentConfig, Semaphore configSem)
	{
		this.log = log;
		this.currentConfig = currentConfig;
		this.configSem = configSem;
		this.folderPath = folderPath;
		queue = new SynchronousQueue<String>(true);
	}
	
	public void run ()
	{
		new Thread(new ScanFolder(log, folderPath, queue)).start();
		
		while (true)
		{
			//Checks if to start camera
			checkIfToStartCamera();
			
			//Checks for new configs or lectures
			try 
			{
				String filePath = queue.poll(10, TimeUnit.MINUTES); //timeout 10min
				log.print("Found file in server input folder, File: " + filePath);
				if (filePath.contains("server_settings.xml"))
				{
					updateConfig(filePath);
				}
				if (filePath.contains("server_lecture.xml"))
				{
					insertLecture(filePath);
				}
				
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
			} 
		}
	}
	
	
	//Private methods below
	
	/**
	 * Updates the config file
	 * @param xmlFilePath Where the new config file is located
	 */
	private void updateConfig (String xmlFilePath)
	{
		try 
		{
			while(!isCompletelyWritten(xmlFilePath))
			{
				sleep(intervalBetweenTries);
			}
			
			configSem.acquire();
			log.print("Starting to update config file");
			EncodeDecodeXml newConfig = new EncodeDecodeXml(xmlFilePath, log);
			currentConfig.addLectureHall(newConfig.readLectureHall());
			currentConfig.addServerIp(newConfig.readServerIp());
			currentConfig.addServerName(newConfig.readServerName());
			currentConfig.addServerPassword(newConfig.readServerPassword());
			log.print("Updated config file");
			log.write(true, "[Success] Network-NetworkRasPiServerInputs; Updated config file");
			configSem.release();
			
			File newConfigFile = new File(xmlFilePath);
			newConfigFile.delete();
			
		} 
		catch (InterruptedException e)
		{
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
		}
	}
	
	private void insertLecture (String xmlFilePath)
	{
		try 
		{
			while(!isCompletelyWritten(xmlFilePath))
			{
				sleep(intervalBetweenTries);
			}
		
			EncodeDecodeXml newLecture = new EncodeDecodeXml(xmlFilePath, log);
			newLecture.readCourseCode(); //TODO insert into schedule class
			newLecture.readLectureTime(); //TODO insert into schedule class
		} 
		catch (InterruptedException e)
		{
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
		}
	}
	
	/**
	 * Checks if the camera controller should start or not
	 */
	private void checkIfToStartCamera ()
	{
		//Need timeedit to be working or have correct api!
	}
	
	/**
	 * Start the camera controller. Will try to start it until it succeeds.
	 */
	private void startCameraController ()
	{
		try
		{
			Process externProgram;
			externProgram = Runtime.getRuntime().exec("ABC");
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
		} //TODO add cameraController startup command		
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
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
			return false;
		}
	}	
	
}
