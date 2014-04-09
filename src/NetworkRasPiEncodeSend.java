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
	TimeEdit schema;
	
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
									SynchronousQueue<String> queue,
									TimeEdit schema)
	{
		this.log = log;
		this.xmlFolderPath = xmlFolderPath;
		this.configReader = configReader;
		this.configSem = configSem;
		this.queue = queue;
		this.schema = schema;
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
				if (xmlFilePath.equals(""))
				{
					log.print("No lecutre at time found: " + imageName.substring(0, 19));
					new File(imageFilePath).delete();
				}
				else
				{
					log.print("Encoded xmlfile at: " + xmlFilePath);
					
					log.print("Sending xmlfile:");
					new NetworkSender (log, configReader, configSem).sendFile(xmlFilePath);
					log.print("Sent xmlfile");
					
					new File(xmlFilePath).delete();
					new File(imageFilePath).delete();
					log.print("Deleted old files");
				}
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
		try 
		{
			configSem.acquire();
			
			EncodeDecodeXml xmlEditor = new EncodeDecodeXml(log);
			String xmlName = xmlFolderPath + configReader.readRasPiId() + "_" + imageName.split("\\.")[0] + ".xml";
			xmlEditor.createNewXml(xmlName, "data-package");
			
			String courseCode;
			if ((courseCode = schema.getCourseCode(imageName.substring(0, 19))).equals(""))
			{
				configSem.release();
				log.write(false, "[WARNING] Network-NetworkRasPiEncodeSend; No lecture is at time: " + imageName.substring(0, 19));
				new File(xmlName).delete();
				return "";
			}
			else
			{
				schema.setLectureHall(configReader.readLectureHall());
				xmlEditor.addRasPiId(configReader.readRasPiId());
				xmlEditor.addLectureHall(configReader.readLectureHall());
//				xmlEditor.addCourseCode("ABC123"); //TODO use timeedit class
//				xmlEditor.addLectureName("DataBASER"); //TODO use timeedot class
				xmlEditor.addCourseCode(courseCode);
				xmlEditor.addLectureName(schema.getCourseName(imageName.substring(0, 19)));
			//	xmlEditor.addTimeStamp(imageName.substring(0, 19)); //Change here if fileName for images changes!!!
//				xmlEditor.addLectureTime("2012-06-06 10:00:00;2012-06-06 11:45:00"); //TODO use timeedit class
				xmlEditor.addLectureTime(schema.getLectureTime(imageName.substring(0, 19)));
				configSem.release();
				xmlEditor.encodeImage(imageFilePath);
				return xmlName;
			}
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkRasPiEncodeSend; " + e.getMessage());
			return "";
		}
	}
}
