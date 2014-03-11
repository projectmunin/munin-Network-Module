import java.io.File;
import java.util.concurrent.Semaphore;
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
	Semaphore configSem;
	
	/**
	 * Contructor
	 * @param log The log object
	 * @param xmlFolderPath	The xml folder path
	 * @param configReader Where configs should be read
	 * @param queue The queue
	 */
	public NetworkRasPiEncodeSend (Log log, 
									String xmlFolderPath, 
									EncodeDecodeXml configReader,
									Semaphore configSem,
									SynchronousQueue<String> queue)
	{
		this.log = log;
		this.xmlFolderPath = xmlFolderPath;
		this.configReader = configReader;
		this.configSem = configSem;
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
				log.print("Noticed incoming file from queue: \"" + imageFilePath + "\"");
				String imageName = imageFilePath.substring(imageFilePath.lastIndexOf("/") + 1); //The one is for imageName dosent get a / in the name
				
				log.print("Starting to create xmlfile");
				String xmlFilePath = encodeXmlFile(imageFilePath, imageName);
				log.print("Encoded xmlfile at: " + xmlFilePath);
				
				log.print("Sending xmlfile:");
				new NetworkSender (log, configReader, configSem).sendFile(xmlFilePath);
				log.print("Sent xmlfile");
				
//				deleteOldFiles(xmlFilePath, imageFilePath);
//				log.print("Deleted old files");
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
		xmlEditor.createNewXml(xmlName, "data-package");
		
		xmlEditor.addRasPiId(configReader.readRasPiId());
		xmlEditor.addLectureHall(configReader.readLectureHall());
		xmlEditor.addCourseCode("ABC123"); //TODO use timeedit class
		xmlEditor.addTimeStamp(imageName.substring(0, 19)); //Change here if fileName for images changes!!!
		xmlEditor.addLectureTime("????"); //TODO use timeedit class
		xmlEditor.encodeImage(imageFilePath);
		return xmlName;
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
