import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;;

public class NetworkRasPi extends Thread
{
	static String imageFolderPath;
	static String xmlFolderPath;
	static EncodeDecodeXml configReader;
	static Log log;
	static int maxTries = 100; 
	static int intervallBetweenTries = 10000; //10sec
		
	
	public static void main(String[] args)
	{
		//Sets folder and Log class
		log = new Log("D:/test/log/", true);  //Log folder
		imageFolderPath = "D:/test/image/";
		xmlFolderPath = "D:/test/xml/";
		
		//Loads configfile
		configReader = new EncodeDecodeXml(log);
		configReader.setXmlFileLocation("D:/test/config/config.xml"); // config file location
		
		runProgram();
		

		
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
	private static void runProgram ()
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
					Path imageName = ev.context();
					String imageFilePath = dir.resolve(imageName).toString().replace("\\", "/");
					while (!isCompletelyWritten(imageFilePath))
					{
						sleep(1000); // Thread sleep so not eat all cpu power.
					}
					System.out.println(imageFilePath); //TODO REMOVE
					
					String xmlFilePath = encodeXmlFile(imageFilePath, imageName.toString());
					
					sendFile(xmlFilePath);
					System.out.println("Sent file"); //TODO REMOVE
					
					File imageFile = new File(imageFilePath);
					imageFile.delete();
					File xmlFile = new File(xmlFilePath);
					xmlFile.delete();
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
	 * Creates new xml and adds relevent data to xmlfile like imagedata.
	 * @param imageFilePath The path for the image file
	 * @param imageName The name of the image file
	 * @return The name of the xmlfile that was created
	 */
	private static String encodeXmlFile (String imageFilePath, String imageName)
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(log);
		String xmlName = xmlFolderPath + configReader.readRasPiId() + "_" + imageName.split("\\.")[0] + ".xml";
		xmlEditor.createNewXml(xmlName);
		System.out.println("xmlfile created"); //TODO REMOVE
		
		xmlEditor.addRasPiId(configReader.readRasPiId());
		xmlEditor.addLectureHall(configReader.readLectureHall());
		xmlEditor.addCourseCode("ABC123"); //TODO use timeedit class
		xmlEditor.addTimeStamp(imageName.substring(0, 19)); //Change here if fileName for images changes!!!
		xmlEditor.addLectureTime("????"); //TODO use timeedit class
		xmlEditor.encodeImage(imageFilePath);
		return xmlName;
	}
	
	/**
	 * Sends the input file to the server defined in the configfile. 
	 * @param xmlFilePath xmlfile Location
	 */
	private static void sendFile (String xmlFilePath)
	{ 
		try
		{
			NetworkClient client = new NetworkClient(log, 
													configReader.readServerIp(), 
													configReader.readServerFolder(), 
													configReader.readServerPassword(), 
													configReader.readServerName());
			System.out.println("sending file: " + xmlFilePath); //TODO remove
			
			int tries;
			for (tries = 0; tries < maxTries; tries++)
			{
				if (client.sendFile(xmlFilePath))
				{
					break;
				}
				sleep(intervallBetweenTries);
			}
			if (tries >= maxTries)
			{
				log.write(false, "[ERROR] Network-NetworkRasPi; Tried " + tries + 
						" times to send file: \"" + xmlFilePath + "\" To: " + configReader.readServerIp());
				//TODO Make something that handles what will happen if we tried sending file to much?
			} 
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPi; " + e.getMessage());
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
}









