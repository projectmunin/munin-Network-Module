import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	Thread senderProcess; //Only here so we dosen't create alots of threads sending configs to server
	TimeEdit schema;
	String cameraControllerPath = "fix"; //TODO FIX
	
	int intervalBetweenTries = 500; //0.5seconds
	
	public NetworkRasPiServerInputs (Log log, String folderPath, EncodeDecodeXml currentConfig, Semaphore configSem, Thread senderProcess, TimeEdit schema)
	{
		this.log = log;
		this.currentConfig = currentConfig;
		this.configSem = configSem;
		this.folderPath = folderPath;
		this.senderProcess = senderProcess;
		queue = new SynchronousQueue<String>(true);
		this.schema = schema;
	}
	
	public void run ()
	{
		new Thread(new ScanFolder(log, folderPath, queue)).start();
		
		while (true)
		{
			//Start camera controller if it should
			startCameraController();
			
			//Checks if to send RasPi configs
			checkIfToSendConfigs();
			
			//Checks for new configs or lectures
			try 
			{
				String filePath = queue.poll(9, TimeUnit.MINUTES); //timeout 9min
				if (filePath != null)
				{
					log.print("Found file in server input folder, File: " + filePath);
					if (filePath.contains("server_config.xml"))
					{
						updateConfig(filePath);
					}
					else 
					{
						File strangeFile = new File(filePath);
						strangeFile.delete();
						log.write(false, "[WARNING] Network-NetworkRasPiServerInputs; Strange file was detected in serverInput folder");
					}
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
			while(!(new isCompletelyWritten().check(xmlFilePath)))
			{
				sleep(intervalBetweenTries);
			}
			
			
			log.print("Starting to update config file");
			EncodeDecodeXml newConfig = new EncodeDecodeXml(xmlFilePath, log);
			configSem.acquire();
			if (!newConfig.readLectureHall().equals(""))
			{
				currentConfig.addLectureHall(newConfig.readLectureHall());
			}
			if (!newConfig.readServerIp().equals(""))
			{
				currentConfig.addServerIp(newConfig.readServerIp());
			}
			if (!newConfig.readServerName().equals(""))
			{
				currentConfig.addServerName(newConfig.readServerName());
			}
			if (!newConfig.readServerPassword().equals(""))
			{
				currentConfig.addServerPassword(newConfig.readServerPassword());
			}
			configSem.release();
			log.print("Updated config file");
			log.write(true, "[SUCCESS] Network-NetworkRasPiServerInputs; Updated config file");
			
			File newConfigFile = new File(xmlFilePath);
			newConfigFile.delete();
			
			//Sending new RasPi configs to server
			if (!senderProcess.isAlive())
			{
				senderProcess = new Thread(new NetworkSender(log, currentConfig, configSem)); 
				senderProcess.start();
			}
		} 
		catch (InterruptedException e)
		{
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
		}
	}
	
	/**
	 * Sends current configs to server at 3'a clock in the night
	 */
	private void checkIfToSendConfigs ()
	{		
		DateFormat currentTime = new SimpleDateFormat("HH:mm");
		Date date = new Date();
		if (!senderProcess.isAlive() && currentTime.format(date).toString().contains("03:1")) //Change here if want to send configs att diffrent time
		{
			//Sending new RasPi configs to server
			log.print("Sendning current config file to server, wont print done message in terminal");
			log.write(true, "[Success] Network-NetworkRasPiServerInputs; Sending current config file");
			senderProcess = new Thread(new NetworkSender(log, currentConfig, configSem));
			senderProcess.start(); 
		}
	}
	
	
	/**
	 * Start the camera controller if it should
	 */
	private void startCameraController ()
	{
		try
		{
			configSem.acquire();
			schema.setLectureHall(currentConfig.readLectureHall());
			int runTime = schema.getLecturesActiveTime()/60;
			configSem.release();
			log.write(true, "[Success] Network-NetworkRasPiServerInputs; The camera controller starts for: "
																						+ runTime + " min");
			if (runTime != 0)
			{
				Process externalProgram;
				do 
				{
					externalProgram = Runtime.getRuntime().exec(cameraControllerPath + " -b 3 -c 3 -r " + runTime); //TODO fix command
					externalProgram.wait(180000); //Waits for 3min until it moves on, and error should have occured in 3min
				} while (externalProgram.exitValue() == 1);			
			}
		} 
		catch (IOException e)
		{
			//Will get here if the program haven't exits get. It should 
			//get here if program starts correctly. 
		} 	
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiServerInputs; " + e.getMessage());
		}
	}
}
