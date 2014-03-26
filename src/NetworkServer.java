import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;


/**
 * Main program for Server. Scans a folder, if notice file sends it to one of the thread 
 * for decoding and inserting into the data base
 * 
 * @author P. Andersson
 *
 */
public class NetworkServer 
{
	//Permanent configs
	static String xmlFolderPath;
	static String imageFileSavePath;
	static Log log;
	static Boolean debugMode = false;
	static SynchronousQueue<String> queue;
	static int numberOfCores = 4;
	
	/**
	 * Initialize the program with start data like folder paths
	 * @param args Arguments
	 */
	public static void main(String[] args)
	{
		//Reads arguments
		readInput(args);
		
		//Sets folder and Log class
		log = new Log("/home/panda/test/log/", debugMode);  //Log folder
		xmlFolderPath = "/home/panda/test/receivedXml/";
		imageFileSavePath = "/home/panda/test/savedImages/";
		
		//Init SynchQueue
		queue = new SynchronousQueue<String>(true);
		
		//Init semaphors
		Semaphore databaseAccess = new Semaphore(1, true);
		
		//Starts threads and folder scanner
		for(int i=0; i<numberOfCores; i++)
		{
			new Thread(new NetworkServerDecodeSave(log, imageFileSavePath, queue, databaseAccess)).start();
		}
		
		new ScanFolder(log, xmlFolderPath, queue).start();
	}
	
	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
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
			else if (args[i].equals("-n") || args[i].equals("--numberOfCores"))
			{
				try
				{
					numberOfCores = Integer.parseInt(args[i+1]);
					i++;
				} 
				catch (NumberFormatException e) 
				{
					System.out.println("ERROR: \"" + args[i+1] + " is not a number");
					System.exit(0);
				}
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
		System.out.println("###### NetworkServer Help message ######");
		System.out.println(" -d OR --deubg              :Enables debug mode");
		System.out.println(" -n OR --numberOfCores INT  :Number of corse the program has to use\n");
		System.out.println(" -h OR --help               :Displays this message\n");
		System.exit(0);
	}
}
