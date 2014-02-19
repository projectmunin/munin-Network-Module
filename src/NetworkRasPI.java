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
	static String folderLocation;
	static String ip;
	static int port;
	static Log log;
		
	
	public static void main(String[] args) throws IOException, InterruptedException 
	{
		//Log things
		log = new Log("D:/test/Network.log", true);
		folderLocation = "D:/test/";
		
		//runProgram();
		

		
		//EncodeDecode test
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(log);
		//xmlEditor.setXmlFileLocation("D:/test/testEditor.xml");
		
		//Adds data and create xmlfile
		xmlEditor.createNewXml("D:/test/testEditor.xml");
	    xmlEditor.addRaspId("ChalmersHC2");
	    xmlEditor.addLectureHall("hc2");
	    xmlEditor.addCourseCode("TDA123");
	    xmlEditor.addTimeStamp("2014");
	    xmlEditor.addLectureTime("NEVER");
		xmlEditor.encodeImage("D:/test/largeImage.bmp");
		xmlEditor.decodeImage("D:/test/rLargeImage.bmp");
		
		
		//Read test
		System.out.println("Data read: " + xmlEditor.readRaspId());
		System.out.println("Data read: " + xmlEditor.readLectureHall());
		System.out.println("Data read: " + xmlEditor.readCourseCode());
		System.out.println("Data read: " + xmlEditor.readTimeStamp());
		System.out.println("Data read: " + xmlEditor.readLectureTime());		

     
        //Get the jvm heap size.
        //long heapSize = Runtime.getRuntime().totalMemory();
         
        //Print the jvm heap size.
        //System.out.println("Heap Size = " + heapSize);
	}
	
	private static void runProgram ()
	{
		try 
		{
			//Watcher things
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path dir = FileSystems.getDefault().getPath(folderLocation);
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
					Path fileName = ev.context();
					String FileLocation = dir.resolve(fileName).toString().replace("\\", "/");
					while (!isCompletelyWritten(FileLocation))
					{
						sleep(1000); // Thread sleep so not eat all cpu power.
					}
					System.out.println(FileLocation);
					//Continue write code here
				}
				key.reset();
			}
		} 
		catch (IOException e) 
		{
			log.write(false, "Network-NetworkRasPi; Error: " + e.getMessage());
			System.exit(0);
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "Network-NetworkRasPi; Error: " + e.getMessage());
			System.exit(0);
		}

		

	}
	
	private static void runNetwork()
	{
		//TODO read from config file
		ip = "192.168.0.185";
		port = 55353;
		NetworkClient network = new NetworkClient();
		network.setFileLocation("D:/test/send.xml");
		network.setIp(ip);
		network.setPort(port);
		network.sendFile();
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










