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
		
		folderScanner();
	}
	
	/**
 	 * Scans the image folder continues . If new files gets moved to the folder, adds the file 
 	 * path to the queue.
 	 */
	private static void folderScanner ()
	{
		try 
		{
			//Watcher things
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path dir = FileSystems.getDefault().getPath(imageFolderPath);
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
			
			//Check the folder for images to be added to the queue
			readExistingImages();
			
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
					
					log.print("Notice file in imagefolder " + imageFilePath);
					log.write(true, "[SUCCESS] Network-NetworkRasPi; Found file in image folder: \"" + 
							 														imageFilePath + "\""); 
					queue.put(imageFilePath);
				}
				key.reset();
			}
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPi; " + e.getMessage());
			System.exit(0);
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPi; " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Read the imagefolder for files. I files are found adds them to the queue
	 */
	private static void readExistingImages ()
	{
		File folder = new File(imageFolderPath);
		File[] listOfImages = folder.listFiles();
		
		for(int i = 0; i < listOfImages.length; i++)
		{
			try 
			{
				queue.put(listOfImages[i].getPath().replace("\\", "/"));
				log.write(true, "[SUCCESS] Network-NetworkRasPi; Found file in image folder: \"" + 
												listOfImages[i].getPath().replace("\\", "/") + "\""); 
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-NetworkRasPi; " + e.getMessage());
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










