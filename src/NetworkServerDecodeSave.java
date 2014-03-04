import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.SynchronousQueue;


public class NetworkServerDecodeSave extends Thread implements Runnable 
{
	//Priate class variables
	Log log;
	SynchronousQueue<String> queue;
	String imageFileSavePath;
	int intervalBetweenTries = 500; //0.5secunds
	
	public NetworkServerDecodeSave (Log log, String imageFileSavePath, SynchronousQueue<String> queue)
	{
		this.log = log;
		this.imageFileSavePath = imageFileSavePath;
		this.queue = queue;
	}
	
	public void run ()
	{
		try
		{
			while (true)
			{
				String xmlFilePath = queue.take();
				log.print("Received this xmlfile: \"" + xmlFilePath + "\" from NetworkServer");
				
				while(!isCompletelyWritten(xmlFilePath))
				{
					sleep(intervalBetweenTries);
				}
				
				log.print("Starting to decode xmlfile");
				decodeXmlFile(xmlFilePath); //Should decode, save image at correct position and insert data in database.
				log.print("Decoded image from xmlfile, saved image at correct " + 
											"position and inserted data in database");
				
//				deleteOldFiles(xmlFilePath);
//				log.print("Deleted old files");
			}			
		}
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
		}
	}
	
	private void decodeXmlFile (String xmlFilePath)
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(xmlFilePath, log);
		String imageName =  xmlFilePath.substring(xmlFilePath.lastIndexOf("/") + 1).split("\\.")[0] + ".bmp";
		xmlEditor.decodeImage(imageFileSavePath + imageName); //TODO fix this use database and save at right folder
	}
	
	/**
	 * Checks if a file has been completely written. Uses the linux program lsof. ONLY works for linux
	 * @param file The file that will be checked if it has been written completely
	 * @return true if it has been completely written, otherwise false;
	 */
	private boolean isCompletelyWritten (String filePath)
	{
		try 
		{
			Process plsof = new ProcessBuilder(new String[]{"lsof", "|", "grep", filePath}).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
			if (reader.readLine() == null)
			{
				plsof.destroy();
				reader.close();
				return true;
			}
			plsof.destroy();
			reader.close();
			return false;
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-NetworkServerDecodeSave; " + e.getMessage());
			return false;
		}
	}
//	private boolean isCompletelyWritten (String file)
//	{
//		RandomAccessFile rFile = null;
//		try 
//		{
//			rFile = new RandomAccessFile(file, "rw");
//			rFile.close();
//			return true;
//		}
//		catch (Exception e){}
//		return false;
//	}
	
	
	/**
	 * Deletes inputet files
	 * @param xmlFilePath	Path for the xmlfile
	 * @param imageFilePath Path for the imagefile
	 */
	private void deleteOldFiles (String xmlFilePath)
	{
		File xmlFile = new File(xmlFilePath);
		xmlFile.delete();
		log.write(true, "[SUCCESS] Network-NetworkServerDecodeSave; Deleted old xmlfile: \"" + 
																			xmlFilePath + "\""); 
	}
}
