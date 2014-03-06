import java.io.IOException;


/**
* Handles everything how a file will be sent to destination
* @author P.Andersson
*/
public class NetworkSender
{

	//Private class variables
	private Log log;
	private String ip;
	private String serverFolder;
	private String serverName;
	private String linuxCommand;

	
	/**
	* Constructor, sets default values to private variables.
	*/
	public NetworkSender (Log log)
	{
		this.log = log;
		this.ip = "";
		this.serverFolder = "";
		this.serverName = "";
	}
	
	/**
	 * Constructor
	 * @param log Which log file to write to
	 * @param ip The ip address for the target server
	 * @param serverFolder Where the file should be stored on the server
	 * @param password The servers password
	 * @param serverName The name of the server
	 */
	public NetworkSender (Log log, String ip, String serverFolder, String password, String serverName)
	{
		this.log = log;
		this.ip = ip;
		this.serverFolder = serverFolder;
		this.serverName = serverName;
		this.linuxCommand = "sshpass -p " + password + " scp ";
	}
	
	/**
	 * Sends the input file to the target server with the already set configurations
	 * @param filePath The location of the file
	 * @return returns true if the file was send successfully otherwise false
	 */
	public boolean sendFile (String filePath)
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
	

	//Set methods
	public void setIp (String inputIp)
	{
		this.ip = inputIp;
	}
	
	public void setServerFolder (String serverFolder)
	{
		this.serverFolder = serverFolder;
	}
	
	public void setPassword (String password)
	{
		this.linuxCommand = "sshpass -p " + password + " scp ";
	}
	
	public void setServerName (String serverName)
	{
		this.serverName = serverName;
	}
}

