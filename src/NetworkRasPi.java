import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;

/**
 * Main Program for the Raspberry PI. Start this one when on the Raspberry PI. 
 * @author P. Andersson
 *
 */
public class NetworkRasPi
{	
	//Permanent configs
	static Boolean debugMode;
	
	/**
	 * Initialize the program with start data like folder paths
	 * @param args Arguments
	 */
	public static void main(String[] args)
	{
		//Reads arguments
		readInput(args);
		
		//Sets folder and Log class
		Log log = new Log("/home/panda/test/log/", debugMode);  //Log folder
		String imageFolderPath = "/home/panda/test/image/";
		String xmlFolderPath = "/home/panda/test/xml/";
		String serverInputFolderPath = "/home/panda/test/serverInputs/";
		
		//Loads configfile
		EncodeDecodeXml configReader = new EncodeDecodeXml(log);
		configReader.setXmlFileLocation("/home/panda/test/config/config.xml"); // config file location
		
		//Init SynchQueue
		SynchronousQueue<String> queue = new SynchronousQueue<String>(true);
		
		//Init semaphors
		Semaphore configSem = new Semaphore(1, true);
		
		//Starts threads and folder scanner
		new Thread(new NetworkSender(log, configReader, configSem)).start(); //Only here to send RasPi configs to server at startup
		new Thread(new NetworkRasPiEncodeSend(log, xmlFolderPath, configReader, configSem, queue)).start();
		new Thread(new NetworkRasPiServerInputs(log, serverInputFolderPath, configReader, configSem)).start();

		new ScanFolder(log, imageFolderPath, queue).start();
	}
	
	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
		debugMode = false;
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
			else
			{
				System.out.println("ERROR: \"" + args[i] + "\" Unknow command");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Prints the help message and exits
	 */
	private static void printHelpMessage()
	{
		System.out.println("###### NetworkRasPi Help message ######");
		System.out.println(" -d OR --deubg   :Enables debug mode");
		System.out.println(" -h OR --help    :Displays this message\n");
		System.exit(0);
	}
}










