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
	
	private final int maxTries = 100; 
	private final int intervalBetweenTries = 10000; //10sec
	private final int longSleep = 54000; //15 minutes


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
		sendFile(configReader.getXmlFileLocaton());
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
				log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; Tried " + tries + 
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
			log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; " + e.getMessage());
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
				log.write(false, "[SUCCESS] Network-NetworkClient; Sent file: \"" + 
							filePath + "\" To: " + serverName + "@" + ip + serverFolder);
				return true;
			}
			else
			{
				log.write(false, "[ERROR] Network-NetworkClient; Could not send file: \"" + 
								filePath + "\" To: " + serverName + "@" + ip + serverFolder);
				return false;
			}
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkClient; " + e.getMessage());
			return false;
		}
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkClient; " + e.getMessage());
			return false;
		} 
	}
}

