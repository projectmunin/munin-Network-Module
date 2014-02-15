import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A server class that accepts connection and save the file at target location
 * @author P.Andersson
 *
 */
public class NetworkServer 
{
	//Private class variables
	private int port;
	private String folderLocation;
	
	/**
	 * Empty constructor
	 */
	public NetworkServer ()
	{
		this.port = 0;
		this.folderLocation = "";
	}
	
	/**
	 * Runs the networkserver, accepts new connections when ready. 
	 */
	public void run () 
	{
		try 
		{
			ServerSocket serverSocket = new ServerSocket(port);
			while (true) 
			{
				System.out.println("Sever listening for connections");  //TODO write to logg-file
				Socket connection = serverSocket.accept();
				System.out.println("Server has connection with client that has ip:" + 
										connection.getInetAddress() + " and port:" +  connection.getPort());  //TODO write to logg-file
				
				BufferedOutputStream outputStream =
								new BufferedOutputStream(new FileOutputStream(
									folderLocation + "/tmp" + System.currentTimeMillis() + ".xml")); //change filename here
				InputStream inputData = connection.getInputStream();
				
				byte[] byteArray = new byte[65536]; //Max size of one package

				//SO BIG FILES GET RECEIVED!
				int count;
				while ((count = inputData.read(byteArray)) > 0){
					System.out.println("countSize " + count);
					outputStream.write(byteArray, 0, count);
				}
				//TODO add message and write to logg-file
				outputStream.close();
				connection.close();
			}
		} 
		catch (IOException e) 
		{
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Sets the port number
	 * @param portNumber
	 */
	public void setPort (int portNumber)
	{
		port = portNumber;
	}
	
	/**
	 * Set the folder location where received files will be saved
	 * @param folderLocation
	 */
	public void setFolderLocation (String folderLocation)
	{
		this.folderLocation = folderLocation;
	}

}
