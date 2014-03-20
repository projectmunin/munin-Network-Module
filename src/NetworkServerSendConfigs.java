import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class NetworkServerSendConfigs
{
	//Variables
	static Boolean debugMode;
	static Boolean dummyMode; //TODO rm 
	static String rasPiId;
	static String room;
	static String serverPassword;
	static String serverName;
	
	static String ethernetName = "etho";
	
	//Database info
	static String user = "root";
	static String password = "abc88";
	static String URL = "//localhost/munin";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		//Reads arguments
		readInput(args);
		if (!dummyMode)
		{
			
			//Sets folder and Log class
			Log log = new Log("/home/panda/test/log/", debugMode);  //Log folder Diffrent log file then Main NetworkServer!!!
			String tmpNewConfigFolder = "/home/panda/test/tmpNewConfigs/";
			String targetFolder = "/home/panda/test/serverInputs/";
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
			{
				log.write(true, "[SUCCESS] Network-NetworkServerSendCondfig; Sent new configs to raspi with ID: " + rasPiId);
				log.print("Sent new configs to rasPi with ID " + rasPiId);
				System.exit(0);
			}
			else
			{
				log.write(false, "[ERROR] Network-NetworkServerSendCondfig; Could not send file to rasPi with ID: " + rasPiId);
				log.print("Could not send file to rasPi with ID: " + rasPiId);
				System.exit(1);
			}
		}
	}

	//TODO fix arguments
	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
		debugMode = false;
		dummyMode = false; //TODO rm
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
				rasPiId = args[i];
				System.out.println("derp");
			}
			else if (args[i].equals("-l") || args[i].equals("--lectureHall"))
			{
				i++;
				room = args[i];
			}
			else if (args[i].equals("-n") || args[i].equals("--name"))
			{
				i++;
				serverName = args[i];
			}
			else if (args[i].equals("-p") || args[i].equals("--password"))
			{
				i++;
				serverPassword = args[i];
			}
			else if (args[i].equals("-m") || args[i].equals("--dummyMode"))
			{
				dummyMode = true;
			}
			else
			{
				System.out.println("ERROR: \"" + args[i] + "\" Unknow command");
				System.exit(2);
			}
			
			if (rasPiId == null)
			{
				System.out.println("ERROR: You must specified rasPi Id" );
				System.exit(5);
			}
		}
	}
	
	/**
	 * Prints the help message and exits
	 */
	private static void printHelpMessage()
	{
		System.out.println("###### NetworkRasPi Help message ######");
		System.out.println(" -d OR --deubg                :Enables debug mode");
		System.out.println(" -h OR --help                 :Displays this message\n");
		System.out.println(" -r OR --rasPiId     'String' :The rasPiId. Must be specified");
		System.out.println(" -l OR --lectureHall 'String  :The new lecture hall");
		System.out.println(" -n OR --name        'String' :New server name");
		System.out.println(" -p OR --password    'String' :New server password");
		System.out.println(" -m OR --dummyMode            :Dummy mode wont send files only error numbers");
		System.exit(0);
	}	
}
