import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class NetworkRasPI 
{
	static String folderLocation;
	static String ip;
	static int port;
	
	public static void main(String[] args) throws IOException 
	{
		//runNetwork();
		File logFile = new File("Network.log");
		PrintWriter pwLogFile = new PrintWriter(new FileWriter(logFile));
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(pwLogFile);
		//xmlEditor.setXmlFileLocation("E:/test/testEditor.xml");
		xmlEditor.createNewXml("D:/test/testEditor.xml");
	    xmlEditor.addRaspId("ChalmersHC2");
	    xmlEditor.addLectureHall("hc2");
	    xmlEditor.addCourseCode("TDA123");
	    //xmlEditor.addTimeStamp("2014");
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

}
