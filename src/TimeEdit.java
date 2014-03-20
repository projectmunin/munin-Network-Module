import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.io.*;

public class TimeEdit {
	
public static void main(String[] args) {
	URLdownload("https://se.timeedit.net/web/chalmers/db1/public/ri157XQQ718Z50Qv37063gZ6y5Y7003Q5Y11Y6.html");
	LectureIDSearch("2014-03-18", 11, 24);
	LectureIDSearch("Lecture ID", 11, 17);
	URLdownload("https://se.timeedit.net/web/chalmers/db1/public/ri.html?h=f&sid=3&p=0.m%2C20140630.x&objects=162288.186&ox=0&types=0&fe=0&id=454132&fr=t&step=0");
	LectureIDSearch("2014-03-18", 11, 24);
}

private static void download(URL input, File output)
	throws IOException {
    	InputStream in = input.openStream();
    	try {
    		OutputStream out = new FileOutputStream(output);
    		try {
    			copy(in, out);
    		} 
    		finally {
    			out.close();
    		}
    	} 
    	finally {
    		in.close();
    	}
  	}

  /**
 * @param in
 * @param out
 * @throws IOException
 * 
 */
  

  
private static void copy(InputStream in, OutputStream out)
	throws IOException {
    	byte[] buffer = new byte[1024];
    	while (true) {
    		int readCount = in.read(buffer);
    		if (readCount == -1) {
    			break;
    		}
    		out.write(buffer, 0, readCount);
    	}
  	}

//public static void main(String[] args) {
//	try {
//		URL url = new URL("https://se.timeedit.net/web/chalmers/db1/public/ri157XQQ718Z50Qv37063gZ6y5Y7003Q5Y11Y6.html");
//		File file = new File("data");
//		download(url, file);
//	} 
//		catch (IOException e) {
//		e.printStackTrace();
//	}
//	LectureIDSearch("2014-03-18", 11, 24);
//}

public static void URLdownload(String args) {
	try {
		String urlIn;
		urlIn = args;
//		System.out.println(urlIn);
		URL url = new URL(urlIn);
		File file = new File("data");
		download(url, file);
	} 
		catch (IOException e) {
		e.printStackTrace();
	}
	
}

public static void LectureIDSearch (String info, int pointOne, int pointTwo) {
      try {
      	String infoSearch = info;
//      	String IDSearch = "Lecture ID";
          
          // Open the file c:\test.txt as a buffered reader
          BufferedReader bf = new BufferedReader(new FileReader("data"));

          // Start a line count and declare a string to hold our current line.
          String line;
          int linecount = 0;
          
          int strings[] = new int[10];
          int position[] = new int[10];
//          int positionID[] = new int[10];
          
          int index = 0;
          
          String lectureTime[] = new String[10];
//          String lectureID[] = new String[10];
          
          String stringFoundLine[] = new String[10];
          
          // Let the user know what we are searching for
          System.out.println("Searching for " + infoSearch + " in file...");
          
          // Loop through each line, stashing the line into our line variable.
          while (( line = bf.readLine()) != null){
              // Increment the count and find the index of the word
              linecount++;
              int indexfound = line.indexOf(infoSearch);
//              int indexfoundID = line.indexOf(IDSearch);

              // If greater than -1, means we found the word
              if (indexfound > -1) {
                  System.out.println("Word was found at position " + indexfound + " on line " + linecount);
                  stringFoundLine[index] = line;
                  strings[index] = linecount;
                  position[index] = indexfound;
//                  positionID[index] = indexfoundID;
                  lectureTime[index] = (stringFoundLine[index].substring((position[index]+pointOne), (position[index]+pointTwo)));
//                  lectureTime[index] = (stringFoundLine[index].substring((position[index]+11), (position[index]+24)));
//                  lectureID[index] = (stringFoundLine[index].substring((positionID[index]+11), (positionID[index]+17)));
                  index++;
              }
          }
          for (int i=0 ; i < index ; i++){
          	System.out.println(strings[i]);
          	System.out.println(position[i]);
          	System.out.println(stringFoundLine[i]);
          	System.out.println(stringFoundLine[i].substring((position[i]+11), (position[i])+24) );
//          	System.out.println(stringFoundLine[i].substring((positionID[i]+11), (positionID[i])+17) );
          }

          // Close the file after done searching
          bf.close();
      }
      catch (IOException e) {
          System.out.println("IO Error Occurred: " + e.toString());
      }
	}
}

