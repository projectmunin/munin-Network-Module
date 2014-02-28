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

public class NetworkRasPi
{	
	//Permanent configs
	static String imageFolderPath;
	static String xmlFolderPath;
	static EncodeDecodeXml configReader;
	static SynchronousQueue<String> queue;
	static Log log;
	
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
		new Thread(new NetworkRasPiEncodeSend(log, xmlFolderPath, configReader, queue)).start();
		
		folderScanner();


		//EncodeDecode test
//		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(log);
//		//xmlEditor.setXmlFileLocation("D:/test/testEditor.xml");
//		
//		//Adds data and create xmlfile
//		xmlEditor.createNewXml("D:/test/testEditor.xml");
//	    xmlEditor.addRasPiId("ChalmersHC2");
//	    xmlEditor.addLectureHall("hc2");
//	    xmlEditor.addCourseCode("TDA123");
//	    xmlEditor.addTimeStamp("2014");
//	    xmlEditor.addLectureTime("NEVER");
//		xmlEditor.encodeImage("D:/test/largeImage.bmp");
//		xmlEditor.decodeImage("D:/test/rLargeImage.bmp");
//		
//		
//		//Read test
//		System.out.println("Data read: " + xmlEditor.readRasPiId());
//		System.out.println("Data read: " + xmlEditor.readLectureHall());
//		System.out.println("Data read: " + xmlEditor.readCourseCode());
//		System.out.println("Data read: " + xmlEditor.readTimeStamp());
//		System.out.println("Data read: " + xmlEditor.readLectureTime());		

     
        //Get the jvm heap size.
        //long heapSize = Runtime.getRuntime().totalMemory();
         
        //Print the jvm heap size.
        //System.out.println("Heap Size = " + heapSize);
	}
	
	/**
	 * Runs the program. This method mostly check a folder continues for new files. If a new file
	 * encode it to xml and sends the file.
	 */
	private static void folderScanner ()
	{
		try 
		{
			//Watcher things
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path dir = FileSystems.getDefault().getPath(imageFolderPath);
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
			
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










