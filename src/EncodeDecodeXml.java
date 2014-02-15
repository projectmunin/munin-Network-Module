import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import javax.xml.bind.DatatypeConverter;


public class EncodeDecodeXml 
{
	//Private class variables
	private String xmlFileLocation;
	private PrintWriter logFile;
	
	//Set variables should only be change here manually
	final String bodyName = "data-package";
	final int specialForAddingData = 5;
	final int lengthBeforeNoticImage = 200;
	final int packageSize = 2097152; //65536 doesn't go over heap space
	
	//Change here if name of tag wants to be change
	final String raspId = "raspId";
	final String lectureHall = "lectureHall";
	final String courseCode = "courseCode";
	final String timeStamp = "timeStamp";
	final String lectureTime = "lectureTime";
	
	public EncodeDecodeXml (PrintWriter logFile)
	{
		this.xmlFileLocation = "";
		this.logFile = logFile;
	}
	public EncodeDecodeXml (String xmlFileLocation, PrintWriter logFile)
	{
		this.xmlFileLocation = xmlFileLocation;
		this.logFile = logFile;
	}
	
	/**
	 * Sets where the xmlfile is be located
	 * @param xmlFileLocation	
	 */
	public void setXmlFileLocation (String xmlFileLocation)
	{
		this.xmlFileLocation = xmlFileLocation;
	}
	
	/**
	 * Creates new xmlfile and writes main body to xmlfile. If xmlfile already exits, 
	 * deletes old and create new empty xmlfile. Also runs the setXmlFileLocation
	 * @param xmlFileLocation	where the new xmlfile will be created
	 * 
	 * @return true if it worked, otherwise false
	 */
	public boolean createNewXml (String xmlFileLocation)
	{
		this.xmlFileLocation = xmlFileLocation;
		File xmlFile = new File(xmlFileLocation);
		xmlFile.mkdirs();
		if (!xmlFile.delete())
		{
			logFile.println("Couldn't delete file at: " + xmlFileLocation);  //TODO write to logg-file
			return false;
		}
		
		try
		{
			xmlFile.createNewFile();
			System.out.println("Successfully created new xml at: " + xmlFileLocation);  //TODO write to logg-file
			
			PrintWriter xmlFileWriter = new PrintWriter(xmlFileLocation);
			xmlFileWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<" + bodyName + ">\n</" + bodyName + ">");
			xmlFileWriter.close();
			System.out.println("Successfully wrote body structure to xml at: " + xmlFileLocation); //TODO write to logg-file
			return true;
		}
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage());
			return false;
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	//Simple add methods
	public void addRaspId (String id)
	{
		addOrUpdateStringInXml(id, raspId);
	}
	public void addLectureHall (String room)
	{
		addOrUpdateStringInXml(room, lectureHall);
	}
	public void addCourseCode (String code)
	{
		addOrUpdateStringInXml(code, courseCode);
	}
	public void addTimeStamp (String time)
	{
		addOrUpdateStringInXml(time, timeStamp);
	}
	public void addLectureTime (String time) 
	{
		addOrUpdateStringInXml(time, lectureTime);
	}
	
	//Simple Read methods
	public String readRaspId ()
	{
		return readDataWithinTag(raspId);
	}
	public String readLectureHall ()
	{
		return readDataWithinTag(lectureHall);
	}
	public String readCourseCode ()
	{
		return readDataWithinTag(courseCode);
	}
	public String readTimeStamp ()
	{
		return readDataWithinTag(timeStamp);
	}
	public String readLectureTime ()
	{
		return readDataWithinTag(lectureTime);
	}
	
	/**
	 * Encodes the "inputFile" and saves it in targetxmlfile
	 * @param inputFile		Target file to be encoded
	 */
	public void encodeImage (String inputFile)
	{
		//Open target imagefile, if no file reaturns error
		File imageFile = new File(inputFile);
		if (!imageFile.isFile()) 
		{
		    System.out.println("Error: " + inputFile + " is not an existing file"); //TODO write to logg-file
		    imageFile.delete();
		    return;
		}
		
		try 
		{
			//Removes old image line and prepares the new line with start tag.
			findAndRemoveLine("image");
			File xmlFile = new File(xmlFileLocation);
			RandomAccessFile rXmlFile = new RandomAccessFile(xmlFile, "rw");
			
			long fileIndex = xmlFile.length() - bodyName.length() - specialForAddingData; 
			if (fileIndex < 0)
			{	
				System.out.println("Error: Negativ offset " + fileIndex + ", maybe the file at \"" + xmlFileLocation + "\" as not been created corecctly"); //TODO write to logg-file
				xmlFile.delete();
				rXmlFile.close();
				return;
			}
			rXmlFile.seek(fileIndex);
			rXmlFile.writeBytes("\t<image>");
			
			//Writes data to xmlfile as base64 encoding. Writing small string parts at a time
			BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(imageFile));
			int fileLength = longToInt(imageFile.length());
			int bytesSavedCounter = 0;
			while (fileLength > bytesSavedCounter) 
			{
				String encodedSmallPart;
				byte[] imageFileByteArray = new byte[packageSize];
				if (fileLength-bytesSavedCounter > packageSize)
				{
					bInputStream.read(imageFileByteArray, 0, imageFileByteArray.length);
					encodedSmallPart = DatatypeConverter.printBase64Binary(imageFileByteArray);
				}
				else 
				{
					byte[] imageFileByteArraySmall = new byte[fileLength-bytesSavedCounter];
					bInputStream.read(imageFileByteArraySmall, 0, fileLength-bytesSavedCounter);
					encodedSmallPart = DatatypeConverter.printBase64Binary(imageFileByteArraySmall);
				}
				bytesSavedCounter = bytesSavedCounter + packageSize;
				rXmlFile.writeBytes(encodedSmallPart);
			}
			
			//Writes ending tags in xmlfile
			rXmlFile.writeBytes("</image>\n</" + bodyName + ">");
			System.out.println("Successfully encoded image at: \"" + inputFile + "\" to xmlfile at: " + xmlFileLocation); //TODO write to logg-file
			rXmlFile.close();
			bInputStream.close();
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		} 
		catch (IOException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}
	}
	
	/**
	 * Reads and decode image from target xmlfile and save it at outputFileLocation
	 * @param outputFileLocation
	 */
	public void decodeImage (String outputFileLocation)
	{
		try 
		{
			byte[] decodedImage = DatatypeConverter.parseBase64Binary(readDataWithinTag("image"));
			BufferedOutputStream outputStream =
							new BufferedOutputStream(new FileOutputStream(outputFileLocation));
			outputStream.write(decodedImage);
			outputStream.close();
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		} 
		catch (IOException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}
	}
	
	
	
	
	//Private methods
	/**
	 * Adds tag with tagData if not already exists. Otherwise updates the tag with tagData. It 
	 * does all this in the xmlFileLocation. Make sure that createNewXml has run ones before 
	 * running this method.
	 * @param tagData		The data that will be written in the target tag
	 * @param tagName		Target tag name
	 */
	private void addOrUpdateStringInXml (String tagData, String tagName)
	{
		try 
		{
			findAndRemoveLine(tagName);
			File xmlFile = new File(xmlFileLocation);
			RandomAccessFile rXmlFile = new RandomAccessFile(xmlFile, "rw");
			int fileIndex = longToInt(xmlFile.length() - bodyName.length() - specialForAddingData); 
			if (fileIndex < 0)
			{	
				System.out.println("Error: Negativ offset " + fileIndex + ", maybe the file at \"" + xmlFileLocation + "\" as not been created corecctly"); //TODO write to logg-file
				xmlFile.delete();
				rXmlFile.close();
				return;
			}
			rXmlFile.seek(fileIndex);
			rXmlFile.writeBytes("\t<" + tagName + ">" + tagData + "</" + tagName + ">\n");
			rXmlFile.writeBytes("</" + bodyName + ">");
			rXmlFile.close();
			if (tagData.length() > lengthBeforeNoticImage) // only here so the log-file wont be spammed with long strings that represent the image data in base64
			{
				tagData = "image base64 encoding";
			}
			System.out.println("Successfully add data: \"" + tagData + "\" in tag: \"" + tagName + "\" to xmlfile at: " + xmlFileLocation); //TODO write to logg-file
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}	
	}
	
	/**
	 * Finds and remove target tagName. If it does not exist, nothing is change in the file. Checks 
	 * in "xmlFileLocation"
	 * @param tagName
	 */
	private void findAndRemoveLine (String tagName)
	{
		try 
		{
			File originalFile = new File(xmlFileLocation);
			
			if (!originalFile.isFile()) 
			{
			    System.out.println("Error: " + xmlFileLocation + " is not an existing file"); //TODO write to logg-file
			    originalFile.delete();
			    return;
			}
			
			File tmpFile = new File(originalFile.getAbsolutePath() + ".tmp");
			BufferedReader br = new BufferedReader(new FileReader(originalFile));
			PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));
			
			String tmpLine;
			while ((tmpLine = br.readLine()) != null)
			{
				if (!tmpLine.contains("<" + tagName + ">")) 
				{
					pw.println(tmpLine);
					pw.flush();
				}
			}
			pw.close();
			br.close();
			
			if (!originalFile.delete())
			{
				System.out.println("Error: Couldn't delete file at: " + xmlFileLocation);  //TODO write to logg-file
				return;
			}
			
			if (!tmpFile.renameTo(originalFile))
			{
				System.out.println("Error: Couldn't rename file at: " + tmpFile.getAbsolutePath());  //TODO write to logg-file
			}
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}
		catch (IOException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return;
		}
	}
	
	/**
	 * Reads the data within the tag and returns the data as a string. If tag was not found
	 * returns an empty string. If something went wrong returns "error"
	 * @param tagName	Target tag
	 * @return	data within the target tag
	 */
	private String readDataWithinTag (String tagName)
	{
		try 
		{
			File xmlFile = new File(xmlFileLocation);
			if (!xmlFile.isFile()) 
			{
			    System.out.println("Error: " + xmlFileLocation + " is not an existing file"); //TODO write to logg-file
			    xmlFile.delete();
			    return "error";
			}
			
			BufferedReader br = new BufferedReader(new FileReader(xmlFile));
			
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.contains("<" + tagName + ">")) 
				{
					br.close();
					System.out.println("Successfully read data from tag: \"" + tagName + "\" from xmlfile at: " + xmlFileLocation); //TODO write to logg-file
					return line.substring(tagName.length() + 3, (line.length() - tagName.length() - 3));
					//The number 3 for the first part is the characters \t, < and >. The second
					//number 3 is for the characters <, /, and >.
				}
			}
			br.close();
			System.out.println("Warning: The tag: \"" + tagName + "\" dosen't exists in xmlfile at: " + xmlFileLocation); //TODO write to logg-file
			return "";
		} 
		catch (FileNotFoundException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return "error";
		}
		catch (IOException e) 
		{
			System.out.println(e.getMessage()); //TODO write to logg-file
			return "error";
		}
	}
	

	/**
	 * Safely converts long to int. Throws IllegalArgumentExcteption if it doesnt work
	 * @param number	Number to be converted
	 * @return The converted number as int
	 */
	private int longToInt (long number) 
	{
		if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
		{
			throw new IllegalArgumentException (number + "Cant convert from long to int");
			//TODO print in LOGG-file
		}
		return (int) number;
	}	
}
