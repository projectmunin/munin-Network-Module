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
	
	/**
	 * Initialize the program with start data like folder paths
	 * @param args Arguments
	 */
	public static void main(String[] args)
	{
		//Sets folder and Log class
		log = new Log("D:/test/log/", true);  //Log folder
		imageFolderPath = "D:/test/image/";
		xmlFolderPath = "D:/test/xml/";
		
		//Loads configfile
		configReader = new EncodeDecodeXml(log);
		configReader.setXmlFileLocation("D:/test/config/config.xml"); // config file location
		
		//Init SynchQueue
		queue = new SynchronousQueue<String>(true);
		
		//Starts threads and folder scanner
//		new Thread(new NetworkRasPiEncodeSend(log, xmlFolderPath, configReader, queue)).start();
//		
//		folderScanner();
		readExistingImages();
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
					
					System.out.println("Notice file in imagefolder"); //TODO remove
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
				queue.put(listOfImages[i].getPath());
				log.write(true, "[SUCCESS] Network-NetworkRasPi; Found file in image folder: \"" + 
																		listOfImages[i].getPath() + "\""); 
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-NetworkRasPi; " + e.getMessage());
			}

		}
	}
	/**
	 * Checks if a file has been completely written 
	 * @param file The file that will be checked if it has been written completely
	 * @return true if it has been completely written, otherwise false;
	 */
	private static boolean isCompletelyWritten (String file)
	{
		RandomAccessFile rFile = null;
		try 
		{
			rFile = new RandomAccessFile(file, "rw");
			rFile.close();
			return true;
		}
		catch (Exception e){}
		return false;
	}
	
	/**
	 * Reads the commands inputs in received array string
	 * @param args The array that holds all the commands
	 */
	private static void readInput (String[] args)
	{
		System.out.println(args.length); //TODO REMOVE
		for (int i=0; i < args.length; i++)
		{
			System.out.println(args[i]); //TODO REMOVE
			if (i-1 < 0)
			{
				continue;
			} 
			else if (args[i].charAt(0) == '-' && args[i-1].charAt(0) == '-')
			{
				System.out.println("[CRITICAL-ERROR] Network-NetworkRasPi; Cant have to command after one other. Error part: \"" + args[i-1] + " " + args[i] + "\".  Write --help for help message");
				System.exit(0);
			} 
			else {
				switch (args[i-1]) 
				{
					case "--imageFolder": 	imageFolderPath = args[i];
											System.out.println("Adding path to imageFolder"); //TODO REMOVE
											break;
					case "--help":			printHelpMessage();
											break;
					default:				System.out.println("[CRITICAL-ERROR] Network-NetworkRasPi; Missing command for \"" + args[i] + "\", \"" + args[i-1] + "\" isnt a command. Write --help for help message");
											System.exit(0);
				}
			}
		}
	}
	
	/**
	 * Prints the help message and exits
	 */
	private static void printHelpMessage()
	{
		System.out.println("##### NetworkRasPi Help message #####");
		System.exit(0);
	}
}










