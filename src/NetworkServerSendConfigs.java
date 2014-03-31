import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Sends new config to target Raspbarry Pi
 * 
 * @author P. Andersson
 * 
 */
public class NetworkServerSendConfigs
{
	//Variables
	static Boolean debugMode;
	static String rasPiId = "";
	static String room = "";
	static String serverPassword = "";
	static String serverName = "";
	
	//Internet connection to use
	static String ethernetName = "lo";
	
	//Database info
	static String user = "root";
	static String password = "abc88";
	static String URL = "//localhost/munin";
	
	/**
	 * Runs the whole program.
	 * Reads input -> reads database -> create file to be sent -> send file -> close
	 * @param args Input arguments
	 */
	public static void main(String[] args) 
	{
		//Reads arguments
		readInput(args);
		getName();
		
		//Sets folder and Log class
		Log log = new Log("/home/panda/test/log/", debugMode);  //Log folder Diffrent log file then Main NetworkServer!!!
		String tmpNewConfigFolder = "/home/panda/test/tmpNewConfigs/";
		String targetFolder = ":/home/panda/test/serverInputs/"; //DONT forgett : at start
		String filePath = tmpNewConfigFolder + rasPiId + "_server_config.xml";
		
		//Creates new configfile
		EncodeDecodeXml configFile = new EncodeDecodeXml(tmpNewConfigFolder, log);
		configFile.createNewXml(filePath, "config");
		
		configFile.addLectureHall(room);
		configFile.addServerIp(new getCurrentIp().get(ethernetName));
		configFile.addServerName(serverName);
		configFile.addServerPassword(serverPassword);
		
		//Gets Raspberry Pi:s ip and password from database
		String rasPiIp = "";
		String rasPiPassword = "";
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager
					.getConnection("jdbc:mysql:" + URL + "?user="+ user + "&password=" + password);
			
			Statement  statement = connect.createStatement();

			ResultSet result = statement.executeQuery("SELECT ip_address, password " +
													"FROM camera_units " +
													"WHERE name = '" + rasPiId + "'");
			if (result.next())
			{
				rasPiIp = result.getString(1);
				rasPiPassword = result.getString(2);
				log.write(true, "[SUCCESS] Network-NetworkServerSendCondfig; Read rasPiIp and password from database");
			}
			else
			{
				log.write(false, "[ERROR] Network-NetworkServerSendCondfig; Cant find a rasPi with ID: " + rasPiId);
				System.exit(3);
			}
		}
		catch (SQLException e)
		{
			log.write(false, "[ERROR] Network-NetworkServerSendCondfig; " + e.getMessage());
			System.exit(4);
		}
		catch (ClassNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerSendCondfig; " + e.getMessage());
			System.exit(4);
		}
		
		//Sends new configs to rasPI
		if (new NetworkSender(log, rasPiIp, targetFolder, rasPiId, rasPiPassword).trySendingFile(filePath))
		//if (new NetworkSender(log, rasPiIp, targetFolder, "panda", rasPiPassword).trySendingFile(filePath)) //TOOD CHANGE BACK
		{
			log.write(true, "[SUCCESS] Network-NetworkServerSendCondfig; Sent new configs to raspi with ID: " + rasPiId);
			log.print("Sent new configs to rasPi with ID " + rasPiId);
			System.exit(0);
		}
		else
		{
			log.write(false, "[ERROR] Network-NetworkServerSendCondfig; Could not send file to rasPi with ID: " + rasPiId);
			log.print("Could not send file to rasPi with ID: " + rasPiId);
			System.exit(8);
		}
	}

	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
		debugMode = false;
		
		if (args.length == 0)
		{
			System.out.println("ERROR: You must write arguments" );
			System.exit(7);
		}
		
		for (int i=0; i < args.length; i++)
		{	
			if (args[i].equals("-h") || args[i].equals("--help"))
			{
				printHelpMessage();
			}
			else if (args[i].equals("-d") || args[i].equals("--debug"))
			{
				debugMode = true;
			}
			else if (args[i].equals("-r") || args[i].equals("--rasPiId"))
			{
				i++;
				outOfBound(args, i);
				rasPiId = args[i];
			}
			else if (args[i].equals("-l") || args[i].equals("--lectureHall"))
			{
				i++;
				outOfBound(args, i);
				room = args[i];
			}
			else if (args[i].equals("-p") || args[i].equals("--password"))
			{
				i++;
				outOfBound(args, i);
				serverPassword = args[i];
			}
			else
			{
				System.out.println("ERROR: \"" + args[i] + "\" Unknow command");
				System.exit(2);
			}
			
			if (rasPiId.equals(""))
			{
				System.out.println("ERROR: You must specified rasPi Id" );
				System.exit(5);
			}
		}
	}
	
	/**
	 * Prints the help message and exits
	 */
	private static void printHelpMessage ()
	{
		System.out.println("###### NetworkServerSendConfigs Help message ######");
		System.out.println(" -d OR --deubg                :Enables debug mode");
		System.out.println(" -h OR --help                 :Displays this message\n");
		System.out.println(" -r OR --rasPiId     'String' :The rasPiId. Must be specified");
		System.out.println(" -l OR --lectureHall 'String  :The new lecture hall");
		System.out.println(" -p OR --password    'String' :New server password");
		System.exit(0);
	}
	
	/**
	 * Checks you pointer is out of bound
	 * @param args The argrument String list
	 * @param i The current pointer/currnet index
	 */
	private static void outOfBound (String [] args, int i)
	{
		if (i >= args.length)
		{
			System.exit(9);
		}
	}
	
	/**
	 * Read the server/computer name and assign serverName to the result. Only works on linux
	 */
	private static void getName ()
	{
		try 
		{
			Process plsof = new ProcessBuilder(new String[]{"hostname"}).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
			serverName = reader.readLine();
			if (serverName.equals(""))
			{
				plsof.destroy();
				reader.close();
				System.exit(6);
			} 
			plsof.destroy();
			reader.close();
		} 
		catch (IOException e) 
		{
			System.exit(6);
		}
	}
}
