import java.io.File;
import java.io.IOException;
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
	private final String ethernetName = "eth0";


	/**
	 * Constructor
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
	
	/**
	 * Constructor, use when you already know ip and other data
	 * @param log Which log file to write too
	 * @param ip The target ip address
	 * @param serverFolder Target folder
	 * @param serverName The name of the target computer
	 * @param password Password for the target computer
	 */
	public NetworkSender (Log log, String ip, String serverFolder, String serverName, String password)
	{
		this.log = log;
		this.ip = ip;
		this.serverFolder = serverFolder;
		this.serverName = serverName;
		this.linuxCommand = "sshpass -p " + password + " scp ";;
	}
	
	/**
	 * Creates and send a configfile that the server can read. 
	 */
	public void run ()
	{
		Boolean done = false;
		while (!done)
		{
			log.print("Starting to create config file to be sent and send");
			String newFilePath = createConfigToBeSent();
			done = sendFileReturns(newFilePath);
			new File(newFilePath).delete();
		}
		log.print("Config file sent");
	}
	
	/**
	 * Creates the configfile that will be sent to server
	 * @return The path for the new file
	 */
	private String createConfigToBeSent ()
	{
		//Creating new config with prober name to be sent to server. Will delete file when sent
		try 
		{
			configSem.acquire();
			EncodeDecodeXml newSendConfig = new EncodeDecodeXml(log);
			String newFilePath = configReader.getXmlFileLocaton().split("\\.")[0] + "_" + configReader.readRasPiId() + ".xml";
			newSendConfig.createNewXml(newFilePath, "config");
			newSendConfig.addRasPiId(configReader.readRasPiId());
			newSendConfig.addLectureHall(configReader.readLectureHall());
			//newSendConfig.addRasPiIpAddress("127.0.0.1"); //TODO get ip-address
			newSendConfig.addRasPiPassword(new getCurrentIp().get(ethernetName));
			configSem.release();
			log.write(true, "[SUCCESS] NetworkSender; Config to be sent has been created");
			return newFilePath;
		}
		catch (InterruptedException e)
		{
			log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			return "";
		}				

	}
	
	/**
	 * Same as sendFile but returns true when done and false if it did not 
	 * succeed. Returns false after the long sleep
	 * @param filePath
	 * @return returns true if the file was send successfully otherwise false
	 */
	public boolean sendFileReturns (String filePath)
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
				return false;
			} 
			return true;
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkSender; " + e.getMessage());
			return false;
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
				sendFile(filePath);
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
	public boolean trySendingFile (String filePath)
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

