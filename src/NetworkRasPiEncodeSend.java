import java.io.File;
import java.util.concurrent.SynchronousQueue;


public class NetworkRasPiEncodeSend extends Thread implements Runnable 
{
	String xmlFolderPath;
	EncodeDecodeXml configReader;
	Log log;
	SynchronousQueue<String> queue;
	int maxTries = 100; 
	int intervallBetweenTries = 10000; //10sec
	
	public NetworkRasPiEncodeSend (Log log, String xmlFolderPath, EncodeDecodeXml configReader, SynchronousQueue<String> queue)
	{
		this.log = log;
		this.xmlFolderPath = xmlFolderPath;
		this.configReader = configReader;
		this.queue = queue;
	}
	
	public void run () 
	{
		try
		{
			while (true)
			{
				String imageFilePath = queue.take();
				String imageName = imageFilePath.substring(imageFilePath.lastIndexOf("/") + 1); //The one is for imageName dosent get a / in the name
				
				System.out.println("Starting to create xmlfile"); //TODO remove
				String xmlFilePath = encodeXmlFile(imageFilePath, imageName);
				System.out.println("Encoded xmlfile at: " + xmlFilePath); //TODO remove
				System.out.println("Sending file:"); //TODO remove
				sendFile(xmlFilePath);
				System.out.println("Sent xmlfile"); //TODO remove
				deleteOldFiles(xmlFilePath, imageFilePath);
				System.out.println("Deleted old files xmlfile"); //TODO remove
			}			
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; " + e.getMessage());
		}

	}
	
	/**
	 * Creates new xml and adds relevent data to xmlfile like imagedata.
	 * @param imageFilePath The path for the image file
	 * @param imageName The name of the image file
	 * @return The name of the xmlfile that was created
	 */
	private String encodeXmlFile (String imageFilePath, String imageName)
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(log);
		String xmlName = xmlFolderPath + configReader.readRasPiId() + "_" + imageName.split("\\.")[0] + ".xml";
		xmlEditor.createNewXml(xmlName);
		
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
	private void sendFile (String xmlFilePath)
	{ 
		try
		{
			NetworkClient client = new NetworkClient(log, 
													configReader.readServerIp(), 
													configReader.readServerFolder(), 
													configReader.readServerPassword(), 
													configReader.readServerName());			
			int tries;
            for (tries = 0; !client.sendFile(xmlFilePath) && tries < maxTries; tries++)
            {
                sleep(intervallBetweenTries);
            }
			if (tries >= maxTries)
			{
				log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; Tried " + tries + 
						" times to send file: \"" + xmlFilePath + "\" To: " + configReader.readServerIp());
				//TODO Make something that handles what will happen if we tried sending file to much?
			} 
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; " + e.getMessage());
		}
	}
	
	/**
	 * Deletes inputet files
	 * @param xmlFilePath	Path for the xmlfile
	 * @param imageFilePath Path for the imagefile
	 */
	private void deleteOldFiles (String xmlFilePath, String imageFilePath)
	{
		File imageFile = new File(imageFilePath);
		imageFile.delete();
		File xmlFile = new File(xmlFilePath);
		xmlFile.delete();
		log.write(true, "[SUCCESS] Network-NetworkRasPiEncodeSend; Deleted old image: \"" + 
								imageFilePath + "\" and old xmlfile: \"" + xmlFilePath + "\""); 
	}
}
