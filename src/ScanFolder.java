import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.SynchronousQueue;


public class ScanFolder extends Thread implements Runnable
{
	Log log;
	String folderPath;
	SynchronousQueue<String> queue;
	
	public ScanFolder (Log log, String folderPath, SynchronousQueue<String> queue)
	{
		this.log = log;
		this.folderPath = folderPath;
		this.queue = queue;
	}
	
	/**
	* Scans the folder continues. If new files gets added to the folder, adds the file 
 	 * path to the queue.
	*/
	public void run () 
	{
		try 
		{
			//Watcher things
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path dir = FileSystems.getDefault().getPath(folderPath);
			WatchKey key = dir.register(watcher, ENTRY_CREATE);
			
			//Check the folder for images to be added to the queue
			readExistingFiles();
			
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
					String filePath = dir.resolve(ev.context()).toString().replace("\\", "/");
					
					log.print("Notice file in imagefolder " + filePath);
					log.write(true, "[SUCCESS] Network-ScanFolder; Found file in image folder: \"" + 
																					filePath + "\""); 
					queue.put(filePath);
				}
				key.reset();
			}
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-ScanFolder; " + e.getMessage());
			System.exit(0);
		} 
		catch (InterruptedException e) 
		{
			log.write(false, "[ERROR] Network-ScanFolder; " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Reads the input folder for files. If files are found adds them to the queue
	 */
	private void readExistingFiles ()
	{
		File folder = new File(folderPath);
		File[] listOfFiles = folder.listFiles();
		
		for(int i = 0; i < listOfFiles.length; i++)
		{
			try 
			{
				queue.put(listOfFiles[i].getPath().replace("\\", "/"));
				log.write(true, "[SUCCESS] Network-ScanFolder; Found file in folder: \"" + 
												listOfFiles[i].getPath().replace("\\", "/") + "\""); 
			} 
			catch (InterruptedException e) 
			{
				log.write(false, "[ERROR] Network-ScanFolder; " + e.getMessage());
			}
		}
	}
}
