import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;


public class NetworkRasPiServerInputs extends Thread implements Runnable 
{
	Log log;
	EncodeDecodeXml currentConfig;
	Semaphore configSem;
	String folderPath;
	SynchronousQueue<String> queue;
	
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
			checkIfToStartCamera();
			try 
			{
				String filePath = queue.poll(10, TimeUnit.MINUTES); //timeout 10min
				log.print("Found file in server input folder, File: " + filePath);
				updateConfig (filePath); //TODO add schedule thing
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
			configSem.acquire();
			log.print("Starting to update config file");
			EncodeDecodeXml newConfig = new EncodeDecodeXml(xmlFilePath, log);
			currentConfig.addLectureHall(newConfig.readLectureHall());
			currentConfig.addServerIp(newConfig.readServerIp());
			currentConfig.addServerName(newConfig.readServerName());
			currentConfig.addServerPassword(newConfig.readServerPassword());
			log.print("Update config file");
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
}
