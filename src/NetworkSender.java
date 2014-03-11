import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Semaphore;


/**
* Handles everything how a file will be sent to destination
* @author P.Andersson
*/
public class NetworkSender extends Thread implements Runnable 
{

	//Private class variables
	private Log log;
	private String ip;
	private String serverFolder;
	private String serverName;
	private String linuxCommand;
	
	private EncodeDecodeXml configReader;
	private Semaphore configSem; 
	
	private final int maxTries = 5; 
	private final int intervalBetweenTries = 5000; //5sec
	private final int longSleep = 30000; //30sec //54000; //15 minutes


	/**
	 * Contructor
	 * @param log Which log file to write too
	 * @param configReader Where to read configs
	 * @param configSem The semaphore to be acquired before reading configs.
	 */
	public NetworkSender (Log log, EncodeDecodeXml configReader, Semaphore configSem)
	{
		this.log = log;
		this.configReader = configReader;
		this.configSem = configSem;
	}
	
	public void run ()
	{
		
		if (configReader.getXmlFileLocaton().contains("config.xml"))
		{
			try 
			{
				//Creating new config with prober name to be sent to server. Will delete file when sent
				configSem.acquire();
				String newFilePath = configReader.getXmlFileLocaton().split("\\.")[0] + "_" + configReader.readRasPiId() + ".xml";
				File configFile = new File(configReader.getXmlFileLocaton());
				File newNameConfig = new File(newFilePath);
				if (newNameConfig.length() > 0)
				{
					sleep(1000); //1 second
				}
				Files.copy(configFile.toPath(), newNameConfig.toPath());
				configSem.release();
				
				sendFile(newFilePath);
				newNameConfig.delete();
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			} 
			catch (IOException e) 
			{
				log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			}
		}
		else
		{
			sendFile(configReader.getXmlFileLocaton());
		}
	}
	
	/**
	 * Sends input file. Will try sending until it succeeds. 
	 * @param filePath
	 */
	public void sendFile (String filePath)
	{
		try
		{
			configSem.acquire();
			ip = configReader.readServerIp();
			serverFolder = configReader.readServerFolder(); 
			linuxCommand = "sshpass -p " + configReader.readServerPassword() + " scp ";
			serverName = configReader.readServerName();	
			configSem.release();
			
			int tries;
            for (tries = 0; !trySendingFile(filePath) && tries < maxTries; tries++)
            {
                sleep(intervalBetweenTries);
            }
			if (tries >= maxTries)
			{
				configSem.acquire();
				log.write(false, "[ERROR] Network-NetworkSender; Tried " + tries + 
										" times to send file: \"" + filePath + "\" To: " + 
											configReader.readServerIp() + " Trying agian in " + 
																	longSleep  + " milliseconds");
				configSem.release();
				sleep(longSleep);
				sendFile (filePath);
			} 
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
		}
	}
	
	/**
	 * Sends the input file to the target server with the already set configurations
	 * @param filePath The location of the file
	 * @return returns true if the file was send successfully otherwise false
	 */
	private boolean trySendingFile (String filePath)
	{
		try 
		{
			Process externProgram;
			externProgram = Runtime.getRuntime().exec(linuxCommand + filePath + 
					" " + serverName + "@" + ip + serverFolder);
			if (externProgram.waitFor() == 0)
			{
				log.write(false, "[SUCCESS] Network-NetworkSender; Sent file: \"" + 
							filePath + "\" To: " + serverName + "@" + ip + serverFolder);
				return true;
			}
			else
			{
				log.write(false, "[ERROR] Network-NetworkSender; Could not send file: \"" + 
								filePath + "\" To: " + serverName + "@" + ip + serverFolder);
				return false;
			}
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			return false;
		}
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			return false;
		} 
	}
}

