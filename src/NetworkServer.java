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
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;


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
		log = new Log("D:/test/log/", debugMode);  //Log folder
		xmlFolderPath = "D:/test/receviedXml/";
		imageFileSavePath = "D:/test/savedImages/";
		
		//Init SynchQueue
		queue = new SynchronousQueue<String>(true);
		
		//Starts threads and folder scanner
		for(int i=0; i<numberOfCores; i++)
		{
			new Thread(new NetworkServerDecodeSave(log, imageFileSavePath, queue)).start();
		}
		folderScanner();
	}
	
	/**
	 * Contineus scan folder for new xmlfiles
	 */
	private static void folderScanner ()
	{
		try 
		{
			//Watcher things
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path dir = FileSystems.getDefault().getPath(xmlFolderPath);
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
			
			//Check the folder for xml to be added to the queue
			readExistingXml();
			
			//Main part
			while (true)
			{
				key = watcher.take();
				
				for (WatchEvent<?> event: key.pollEvents()) 
				{
					if (event.kind() == OVERFLOW)
					{
						continue;
					}
					
					WatchEvent<Path> ev = (WatchEvent<Path>)event; //Maybe find safe way?
					String imageFilePath = dir.resolve(ev.context()).toString().replace("\\", "/");
					
					log.print("Notice file in xmlfolder");
					log.write(true, "[SUCCESS] Network-NetworkServer; Found file in xml folder: \"" + 
							 														imageFilePath + "\""); 
					queue.put(imageFilePath); //TODO Add things here
				}
				key.reset();
			}
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServer; " + e.getMessage());
			System.exit(0);
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServer; " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Read the xmlfolder for files. I files are found adds them to the queue
	 */
	private static void readExistingXml ()
	{
		File folder = new File(xmlFolderPath);
		File[] listOfImages = folder.listFiles();
		
		for(int i = 0; i < listOfImages.length; i++)
		{
			try 
			{
				queue.put(listOfImages[i].getPath().replace("\\", "/"));
				log.write(true, "[SUCCESS] Network-NetworkServer; Found file in xml folder: \"" + 
												listOfImages[i].getPath().replace("\\", "/") + "\""); 
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-NetworkServer; " + e.getMessage());
			}
		}
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
