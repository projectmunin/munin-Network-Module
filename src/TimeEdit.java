import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * This class uses timeedit to get course code, course name and more 
 * 
 * @author P. Andersson & M. Undgård
 *
 */
public class TimeEdit extends Thread
{
	//Global variables
	private String room;
	private Calendar lastRefresh;
	private Log log;
	
	//Final variables
	final private int intervallBetweenRefresh = 1; //1 Hour
	final private String mainHtmlFile = "allLecture.html";
	final private String tmpDataStorageMain = "allLecturesData.tmp";
	final private String minScriptPath = "min.js";
	final private String extraScriptPath = "extra.js";
	
	public TimeEdit (Log log, String room)
	{
		try 
		{
			this.log = log;
			this.room = room;
			this.lastRefresh = Calendar.getInstance();
			lastRefresh.add(Calendar.HOUR_OF_DAY, intervallBetweenRefresh);			
			
			//Downloads the html file that conatins all lectures for a room
			URL url = new URL(getRoomURL());
			File file = new File(mainHtmlFile);
			download(url, file);
		} 
		catch (MalformedURLException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
		} 
	}
	
	/**
	 * Returns the start and end time for the lecture input time is in
	 * @param imageTime The time for the image, Syntax 2012-09-11_10:13:00
	 * @return The start and endtime of lecture Syntax 2012-09-11 10:13:00;2012-09-11 10:45:00
	 * Returns empty string if didn't find any lecture time
	 */
	public String getLectureTime (String imageTime)
	{
		String[] lectureTime = new String[10];
		ReadString(imageTime.split("\\_")[0], mainHtmlFile, tmpDataStorageMain);
		lectureTime = ReadData(imageTime.split("\\_")[0], tmpDataStorageMain, 11, 24);
		
		int index = whichLecture(lectureTime, imageTime);
		if (index == -1)
		{
			log.write(false, "[WARNING] TimeEdit; Could not found a lecture time for time: " + imageTime);
			return "";
		}
		
		String startTime = imageTime.split("\\_")[0] + " " + lectureTime[index].split("\\-")[0].trim() + ":00";
		String endTime = imageTime.split("\\_")[0] + " " + lectureTime[index].split("\\-")[1].trim() + ":00";
		log.write(true, "[SUCCESS] TimeEdit; Found lecture with start time: " + startTime  + " and end time " + endTime);
		return startTime + ";" + endTime;
	}
	
	/**
	 * Returns the name for a course
	 * @param The time for the input image, Syntax 2012-09-11_10:13:00
	 * @return THe name for the course. Empty string if not found
	 */
	public String getCourseName (String imageTime)
	{
		String[] lectureTime = new String[10];
		ReadString(imageTime.split("\\_")[0], mainHtmlFile, tmpDataStorageMain);
		
		//Gets number for the lecture, first lecture of day is 0
		lectureTime = ReadData(imageTime.split("\\_")[0], tmpDataStorageMain, 11, 24);
		
		int index = whichLecture(lectureTime, imageTime);
		if (index == -1)
		{
			log.write(false, "[WARNING] TimeEdit; Could not found a course name for image with time: " + imageTime);
			return "";
		}
		
		lectureTime = ReadData(imageTime.split("\\_")[0], tmpDataStorageMain, 25, 100);
		log.write(true, "[SUCCESS] TimeEdit; Found course name: " + lectureTime[index].split("\\,")[0]);
		return lectureTime[index].split("\\,")[0];
	}
	
	/**
	 * Returns the coursecode for the lecture for the given time, Syntax 2012-09-11_10:13:00
	 * @param imageTime Input lecture date
	 * @return The course code as a string, if not found returns null
	 */
	public String getCourseCode (String imageTime)
	{
		try 
		{
			String[] lectureID = new String[10];
			String courseSiteInfo = "";
			String courseCode = "";
			String[] lectureTime = new String[10];
			
			//Gets number for the lecture, first lecture of day is 0
			ReadString(imageTime.split("\\_")[0], mainHtmlFile, tmpDataStorageMain);
			
			lectureTime = ReadData(imageTime.split("\\_")[0], tmpDataStorageMain, 11, 24);
			
			int index = whichLecture(lectureTime, imageTime);
			if (index == -1)
			{
				log.write(false, "[WARNING] TimeEdit; Could not found a course code for time: " + imageTime);
				return "";
			}
			
			//Gets the coursecode
			lectureID = getLectureID(imageTime.split("\\_")[0]);
			
			URL url2 = new URL("https://se.timeedit.net/web/chalmers/db1/public/ri.html?h=f&sid=3&p=0.m%2C20140630.x&objects=162288.186&ox=0&types=0&fe=0&id="+lectureID[index]+"&fr=t&step=0");
			File file2 = new File("lectureBox.html");
			download(url2, file2);
			
			ReadString("objects/2", "lectureBox.html", "lectureBoxData.tmp");
			courseSiteInfo = ReadData("objects/2", "lectureBoxData.tmp", 8, 32)[0];
			
			URL url3 = new URL("https://se.timeedit.net/web/chalmers/db1/public/objects/"+courseSiteInfo);
			File file3 = new File("courseData.html");
			download(url3, file3);
			
			ReadString("data-name", "courseData.html", "courseData.tmp");
			courseCode = ReadData("data-name", "courseData.tmp", 11, 17)[0];	
			
			log.write(true, "[SUCCESS] TimeEdit; Found course code: " + courseCode);
			
			return courseCode;
		} 
		catch (MalformedURLException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		} 		
	}
	
	/**
	 * Redownload the timeedit file if last refresh was 1 hour ago
	 */
	private void refresh ()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			if (currentTime.after(lastRefresh))
			{
				URL url = new URL(getRoomURL());
				File file = new File(mainHtmlFile);
				download(url, file);
				lastRefresh = currentTime;
				lastRefresh.add(Calendar.HOUR_OF_DAY, intervallBetweenRefresh);
			}
		}
		catch (MalformedURLException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
		} 	
	}

	/**
	 * Finds out which of all lecture in a day to chose from. All lecture
	 * gets +14.49min at their end time 
	 * @param lectureTime A string list with all the lecture times
	 * @param imageTime The time which should fit into one of the input lectures. 
	 * @return The index of which lecture that have been chosen.
	 */
	private int whichLecture (String[] lectureTime, String imageTime)
	{
		for (int i=0; i<lectureTime.length && lectureTime[i] != null; i++)
		{
			Calendar foundLectureStart =  Calendar.getInstance();// time to test: 12:15:00
			foundLectureStart.set(0, 0, 0,
					Integer.parseInt(lectureTime[i].substring(0, 2)),
					Integer.parseInt(lectureTime[i].substring(3, 5)), 
					-1);
			
			Calendar foundLectureEnd = Calendar.getInstance(); // for example 12:00:00
			foundLectureEnd.set(0, 0, 0,
					Integer.parseInt(lectureTime[i].substring(8, 10)),
					Integer.parseInt(lectureTime[i].substring(11, 13))+14, 
					59);

			Calendar inputTime = Calendar.getInstance();
			inputTime.set(0, 0, 0,
					Integer.parseInt(imageTime.substring(11, 13)),
					Integer.parseInt(imageTime.substring(14, 16)),
					Integer.parseInt(imageTime.substring(17, 19)));
			
			if(inputTime.after(foundLectureStart) && inputTime.before(foundLectureEnd))
			{
				return i;
			}			
		}
		return -1;
	}

	/**
	 * Find the url with all the lectures for input room
	 * @return The url with all the lectures
	 */
	private String getRoomURL ()
	{
		try 
		{
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("javascript");
			
			URL url = new URL("https://se.timeedit.net/web/chalmers/db1/public/objects.html?max=15&fr=t&partajax=t&im=f&sid=3&l=en_US&search_text=" + room + "&types=186");
			File roomIdFile = new File("roomId.html");
			download(url, roomIdFile);
			
			//Download the html file which contains the id for a room and adds that id to String "id"
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(roomIdFile)));
			String id = "";
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.contains("data-id="))
				{
					id = line.split("data-id=\"")[1].split("\"")[0];
				}
			}
			roomIdFile.delete();
			br.close();
			
			//Uses the script and gets the url for all the lecture for a room
			engine.eval(readFile(minScriptPath, StandardCharsets.UTF_8));
			engine.eval(readFile(extraScriptPath, StandardCharsets.UTF_8));
			engine.eval("var urls = ['https://se.timeedit.net/web/chalmers/db1/public/ri.html', 'h=t&sid=3&p=0.m%2C20140630.x'];");
			engine.eval("var keyValues = ['h=t', 'sid=3', 'p=0.m%2C20140630.x', 'objects=" + id + "', 'ox=0', 'types=0', 'fe=0'];");
			engine.eval("var url = TEScramble.asURL(urls, keyValues);");
			String roomURL = engine.get("url").toString();

			return roomURL;
			
		} 
		catch (ScriptException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		} 
		catch (MalformedURLException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		}
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		}
	}
	
	private String readFile(String path, Charset encoding) 
	{
		try 
		{
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return encoding.decode(ByteBuffer.wrap(encoded)).toString();
		} 
		catch (IOException e)
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
			return "";
		}
	}
	
	/**
	 * Dowloads target URL to output file
	 * @param input input URL
	 * @param output Output file
	 */
	private void download(URL input, File output)
	{
		try 
		{
			InputStream in = input.openStream();
	    	try 
	    	{
	    		OutputStream out = new FileOutputStream(output);
	    		try 
	    		{
	    			copy(in, out);
	    		} 
	    		finally 
	    		{
	    			out.close();
	    		}
	    	} 
	    	finally 
	    	{
	    		in.close();
	    	}
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
		}
	}
	
	/**
	 * Buffrar en instream som sedan skickas till outstream  
	 * @param in InputStream
	 * @param out OutputStream
	 * @throws IOException
	 */
	private void copy(InputStream in, OutputStream out)
		throws IOException 
		{
	    	byte[] buffer = new byte[1024];
	    	while (true) 
	    	{
	    		int readCount = in.read(buffer);
	    		if (readCount == -1) 
	    		{
	    			break;
	    		}
	    		out.write(buffer, 0, readCount);
	    	}
	  	}
	
	private String[] getLectureID (String imageTime)
	{
		
		String[] lectureID = new String[10];
		ReadString(imageTime, mainHtmlFile, tmpDataStorageMain);
		lectureID = ReadData("data-id", tmpDataStorageMain, 9, 15);
		
		return lectureID;
	}
	
	/**
	 * Finds the string within the key word
	 * @param searchWord Key word
	 * @param fileToSearch File to be search
	 * @param printTo 
	 * @return
	 */
	private String[] ReadString (String searchWord, String fileToSearch, String printTo)
	{
	      try 
	      {	    	  
	          BufferedReader bf = new BufferedReader(new FileReader(fileToSearch));
	
	          int linecount = 0;
	          String line;
	          int strings[] = new int[10];
	          int index = 0;
	          String stringFoundLine[] = new String[10];
	          while (( line = bf.readLine()) != null)
	          {
	              linecount++;
	              int indexfound = line.indexOf(searchWord);
	
	              if (indexfound > -1) 
	              {
	                  stringFoundLine[index] = line;
	                  strings[index] = linecount;
	                  index++;
	              }
	          }
	          		  
	          File file = new File(printTo);  
	          PrintWriter out = new PrintWriter(new FileWriter(file));  
	          
	          // Write each string in the array on a separate line  
	          for (String s : stringFoundLine) 
	          {  
	          		out.println(s);  
	          }  
	          		  
	          out.close();
	
	          // Close the file after done searching
	          bf.close();
	          
	          return stringFoundLine;
	      }
	      catch (IOException e) 
	      {
	    	  log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
	          return null;
	      }
	}
	
	/**
	 * Uses to find data in a html file.
	 * @param searchWord Key word to search for
	 * @param fileToSearch The file to search in
	 * @param pointOne Pointer for start substring
	 * @param pointTwo Pointer for end substring
	 * @return A String list of all the relevent data
	 */
	private String[] ReadData (String searchWord, String fileToSearch, int pointOne, int pointTwo)
	{
	      try 
	      {
	    	  String infoSearch = searchWord;
	    	  String fileName = fileToSearch;
	          int p1 = pointOne;
	          int p2 = pointTwo;
	    	  
	          BufferedReader bf = new BufferedReader(new FileReader(fileName));
	
	          String line;
	
	          int position[] = new int[10];
	          int index = 0;
	          String lectureInfo[] = new String[10];
	          String stringFoundLine[] = new String[10];
	          while (( line = bf.readLine()) != null)
	          {
	          	int indexfound = line.indexOf(infoSearch);
	              // If greater than -1, means we found the word
	              if (indexfound > -1) 
	              {
	                  stringFoundLine[index] = line;
	                  position[index] = indexfound;
	                  lectureInfo[index] = (stringFoundLine[index].substring((position[index]+p1), (position[index]+p2)));
	                  index++;
	              }
	          }
	
	          // Close the file after done searching
	          bf.close();
	          return lectureInfo;
	      }
	      catch (IOException e) 
	      {
	    	  log.write(false, "[ERROR] TimeEdit; " + e.getMessage());
	          return null;
	      }
	}
}