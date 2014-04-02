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

public class TimeEdit 
{
	//Global variables
	private String room;
	private Calendar lastRefresh;
	
	public TimeEdit (String room) 
	{
		try 
		{
			this.room = room;
			this.lastRefresh = Calendar.getInstance();
			URL url = new URL(getRoomURL());
			File file = new File("data");
			download(url, file);
		} 
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		ReadString(imageTime.split("\\_")[0], "data", "logfile");
		lectureTime = ReadData(imageTime.split("\\_")[0], "logfile", 11, 24);
		
		int index = whichLecture(lectureTime, imageTime);
		if (index == -1)
		{
			return "";
		}
		
		String startTime = imageTime.split("\\_")[0] + " " + lectureTime[index].split("\\-")[0].trim() + ":00";
		String endTime = imageTime.split("\\_")[0] + " " + lectureTime[index].split("\\-")[1].trim() + ":00";
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
		ReadString(imageTime.split("\\_")[0], "data", "logfile");
		
		//Gets number for the lecture, first lecture of day is 0
		lectureTime = ReadData(imageTime.split("\\_")[0], "logfile", 11, 24);
		
		int index = whichLecture(lectureTime, imageTime);
		if (index == -1)
		{
			return "";
		}
		
		lectureTime = ReadData(imageTime.split("\\_")[0], "logfile", 25, 100); //DESSA fixa positioner :(
		
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
			ReadString(imageTime.split("\\_")[0], "data", "logfile");
			
			lectureTime = ReadData(imageTime.split("\\_")[0], "logfile", 11, 24);
			
			int index = whichLecture(lectureTime, imageTime);
			if (index == -1)
			{
				return "";
			}
			
			//Gets the coursecode
			lectureID = getLectureID(imageTime.split("\\_")[0]);
			
			System.out.println("lecture id: " + lectureID[index]); //TODO RM
			
			URL url2 = new URL("https://se.timeedit.net/web/chalmers/db1/public/ri.html?h=f&sid=3&p=0.m%2C20140630.x&objects=162288.186&ox=0&types=0&fe=0&id="+lectureID[index]+"&fr=t&step=0");
			File file2 = new File("data2");
			download(url2, file2);
			
			ReadString("objects/2", "data2", "logfile2");
			courseSiteInfo = ReadData("objects/2", "logfile2", 8, 32)[0];
			
			URL url3 = new URL("https://se.timeedit.net/web/chalmers/db1/public/objects/"+courseSiteInfo);
			File file3 = new File("data3");
			download(url3, file3);
			
			ReadString("data-name", "data3", "logfile3");
			courseCode = ReadData("data-name", "logfile3", 11, 17)[0];	
			
			return courseCode;
		} 
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} 		
	}
	
	/**
	 * Redownload the downloaded timeedit file if needed
	 */
	private void refresh ()
	{
		Calendar currentTime = Calendar.getInstance();
		if (lastRefresh.)
		{
			
		}
	}

	private int whichLecture (String[] lectureTime, String imageTime)
	{
		for (int i=0; i<lectureTime.length && lectureTime[i] != null; i++)
		{
			Calendar foundTimeStart =  Calendar.getInstance();// time to test: 12:15:00
			foundTimeStart.set(0, 0, 0,
					Integer.parseInt(lectureTime[i].substring(0, 2)),
					Integer.parseInt(lectureTime[i].substring(3, 5)), 
					0);
			
			Calendar foundLectureEnd = Calendar.getInstance(); // for example 12:00:00
			foundLectureEnd.set(0, 0, 0,
					Integer.parseInt(lectureTime[i].substring(8, 10)),
					Integer.parseInt(lectureTime[i].substring(11, 13))+14, 
					59);

			Calendar inputTime = Calendar.getInstance();
			inputTime.set(0, 0, 0,
					Integer.parseInt(imageTime.substring(11, 13)),
					Integer.parseInt(imageTime.substring(14, 16)),
					0);
			
			if(inputTime.after(foundTimeStart) && inputTime.before(foundLectureEnd))
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
			//System.out.println(id); //TODO RM
			
			//Uses the script and gets the url for all the lecture for a room
			engine.eval(readFile("min.js", StandardCharsets.UTF_8)); //CHANGE HERE IF LOCATION CHANGES!!!!
			engine.eval(readFile("extra.js", StandardCharsets.UTF_8)); //CHANGE HERE IF LOCATION CHANGES!!!!
			engine.eval("var urls = ['https://se.timeedit.net/web/chalmers/db1/public/ri.html', 'h=t&sid=3&p=0.m%2C20140630.x'];");
			engine.eval("var keyValues = ['h=t', 'sid=3', 'p=0.m%2C20140630.x', 'objects=" + id + "', 'ox=0', 'types=0', 'fe=0'];");
			engine.eval("var url = TEScramble.asURL(urls, keyValues);");
			String roomURL = engine.get("url").toString();

			//System.out.println(roomURL); //TODO RM
			return roomURL;
			
		} 
		catch (ScriptException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} 
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		ReadString(imageTime, "data", "logfile");
		lectureID = ReadData("data-id", "logfile", 9, 15);
		
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
	      catch (IOException e) {
	          System.out.println("IO Error Occurred: " + e.toString());
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
	private static String[] ReadData (String searchWord, String fileToSearch, int pointOne, int pointTwo)
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
	          System.out.println("IO Error Occurred: " + e.toString());
	          return null;
	      }
	}
}