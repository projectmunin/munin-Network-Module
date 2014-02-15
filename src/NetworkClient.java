import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


/**
 * Handles everything how a file will be sent
 * @author P.Andersson
 *
 */
public class NetworkClient 
{
	//Private class variables
	private String fileLocation;
	private String ip;
	private int port;
	
	/**
	 * Constructor, sets default values to private variables.
	 */
	public NetworkClient ()
	{
		this.fileLocation = "";
		this.ip = "";
		this.port = 0;
	}
	
	/**
	 * Sends a file to the target ip
	 * @return true if the file was sent, otherwise false
	 */
	public boolean sendFile ()
	{
		try 
		{
			Socket sock = new Socket(ip, port);
			System.out.println("Client connected to server with ip:" + ip + " and port:" + port);  //TODO write to logg-file
			File file = new File(fileLocation);
			byte[] fileByteArray = new byte[longToInt(file.length())];
			
			BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(file));
			bInputStream.read(fileByteArray, 0, fileByteArray.length);
			bInputStream.close();
			
			OutputStream sendStream = sock.getOutputStream();
			System.out.println("Sedning file");  //TODO write to logg-file
			sendStream.write(fileByteArray, 0, fileByteArray.length);
			sendStream.flush();
			
			sock.close();
			System.out.println("File sent");  //TODO write to logg-file
			return true;
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Sets which file to be sent
	 * @param fileLocation	Where the file is located 
	 */
	public void setFileLocation (String inputFileLocation)
	{
		fileLocation = inputFileLocation;
	}
	
	/**
	 * Sets the ip-address
	 * @param inputIp	The target ip-address
	 */
	public void setIp (String inputIp)
	{
		ip = inputIp;
	}
	/**
	 * Sets the port number
	 * @param inputPort	The target port number
	 */
	public void setPort (int inputPort) 
	{
		port = inputPort;
	}
	
	//Privates methods below	
	private int longToInt (long number) 
	{
		if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
		{
			throw new IllegalArgumentException (number + "Cant convert from long to int");
			//TODO print in LOGG-file
		}
		return (int) number;
	}
}
