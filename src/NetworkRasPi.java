import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.SynchronousQueue;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;;

/**
 * Main Program for the Raspberry PI. Start this one when on the Raspberry PI. 
 * @author P. Andersson
 *
 */
public class NetworkRasPi
{	
	//Permanent configs
	static String imageFolderPath;
	static String xmlFolderPath;
	static EncodeDecodeXml configReader;
	static SynchronousQueue<String> queue;
	static Log log;
	static Boolean debugMode = false;
	
	/**
	 * Initialize the program with start data like folder paths
	 * @param args Arguments
	 */
	public static void main(String[] args)
	{
		//Reads arguments
		readInput(args);
		
		//Sets folder and Log class
		log = new Log("D:/test/log/", debugMode);  //Log folder
		imageFolderPath = "D:/test/image/";
		xmlFolderPath = "D:/test/xml/";
		
		//Loads configfile
		configReader = new EncodeDecodeXml(log);
		configReader.setXmlFileLocation("D:/test/config/config.xml"); // config file location
		
		//Init SynchQueue
		queue = new SynchronousQueue<String>(true);
		
		//Starts threads and folder scanner
		new Thread(new NetworkRasPiEncodeSend(log, xmlFolderPath, configReader, queue)).start();
		
		new ScanFolder(log, imageFolderPath, queue).start();
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










