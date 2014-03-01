import java.io.File;
import java.util.concurrent.SynchronousQueue;

/**
 * Continues check the queue for new files. If new file get notice, encode it and sends it to
 * the server specified in the configfile.
 * @author P. Andersson
 *
 */
public class NetworkRasPiEncodeSend extends Thread implements Runnable 
{
	String xmlFolderPath;
	EncodeDecodeXml configReader;
	Log log;
	SynchronousQueue<String> queue;
	int maxTries = 100; 
	int intervallBetweenTries = 10000; //10sec
	
	/**
	 * Contructor
	 * @param log The log object
	 * @param xmlFolderPath	The xml folder path
	 * @param configReader Where configs sould be read
	 * @param queue The queue
	 */
	public NetworkRasPiEncodeSend (Log log, 
									String xmlFolderPath, 
									EncodeDecodeXml configReader, 
									SynchronousQueue<String> queue)
	{
		this.log = log;
		this.xmlFolderPath = xmlFolderPath;
		this.configReader = configReader;
		this.queue = queue;
	}
	
	/**
	 * Runs the thread and notice if new file adds to the queue. After that encodes it and sends it 
	 */
	public void run () 
	{
		try
		{
			while (true)
			{
				String imageFilePath = queue.take();
				log.print("Noticed file: \"" + imageFilePath + "\"");
				String imageName = imageFilePath.substring(imageFilePath.lastIndexOf("/") + 1); //The one is for imageName dosent get a / in the name
				
				log.print("Starting to create xmlfile");
				String xmlFilePath = encodeXmlFile(imageFilePath, imageName);
				log.print("Encoded xmlfile at: " + xmlFilePath);
				
				log.print("Sending xmlfile:");
				sendFile(xmlFilePath);
				log.print("Sent xmlfile");
				
				deleteOldFiles(xmlFilePath, imageFilePath);
				log.print("Deleted old files xmlfile");
			}			
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; " + e.getMessage());
		}

	}
	
	
	//Private methods below
	
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
