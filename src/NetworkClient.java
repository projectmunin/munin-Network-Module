import java.io.IOException;


/**
* Handles everything how a file will be sent to destination
* @author P.Andersson
*/
public class NetworkClient
{

	//Private class variables
	//private String password = "server";
	//private String serverFolder = ":~/home/panda";
	//private String serverName = "panda";
	private Log log;
	private String ip;
	private String serverFolder;
	private String password;
	private String serverName;
	private String linuxCommand = "sshpass -p " + password + " scp ";

	
	/**
	* Constructor, sets default values to private variables.
	*/
	public NetworkClient (Log log)
	{
		this.log = log;
		this.ip = "";
		this.serverFolder = "";
		this.password = "";
		this.serverName = "";
	}
	
	/**
	 * Contstructor
	 * @param log Which log file to write to
	 * @param ip The ip address for the target server
	 * @param serverFolder Where the file should be stored on the server
	 * @param password The servers password
	 * @param serverName The name of the server
	 */
	public NetworkClient (Log log, String ip, String serverFolder, String password, String serverName)
	{
		this.log = log;
		this.ip = ip;
		this.serverFolder = serverFolder;
		this.password = password;
		this.serverName = serverName;
	}
	
	/**
	 * Send the inputet file to the target server with the already set configs
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
				log.write(false, "[SECCESS] Network-NetworkClient; Sent file: \"" + 
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
		this.password = password;
	}
	
	public void setServerName (String serverName)
	{
		this.serverName = serverName;
	}
}

