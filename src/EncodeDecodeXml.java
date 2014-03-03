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

/**
 * Handles the xml format for the Raspberry PI
 * @author P. Andersson
 *
 */
public class EncodeDecodeXml 
{
	//Private class variables
	private String xmlFileLocation;
	private Log log;
	
	//Set variables should only be change here manually
	private final String bodyName = "data-package";
	private final int specialForAddingData = 5;
	private final int lengthBeforeNoticImage = 50;
	private final int packageSize = 2097152; //65536 doesn't go over heap space
	
	//Change here if name of tag wants to be change
	private final String rasPiId = "rasPiId";
	private final String lectureHall = "lectureHall";
	private final String courseCode = "courseCode";
	private final String timeStamp = "timeStamp";
	private final String lectureTime = "lectureTime";
	private final String serverIp = "serverIp";
	private final String serverName = "serverName";
	private final String serverFolder = "serverFolder";
	private final String serverPassword = "serverPassword";
	
	/**
	 * Constructor
	 * @param logFile Target logfile
	 */
	public EncodeDecodeXml (Log logFile)
	{
		this.xmlFileLocation = "";
		this.log = logFile;
	}
	/**
	 * Constructor
	 * @param xmlFileLocation xml file path
	 * @param logFile Target logfile
	 */
	public EncodeDecodeXml (String xmlFileLocation, Log logFile)
	{
		this.xmlFileLocation = xmlFileLocation;
		this.log = logFile;
	}
	
	public void setXmlFileLocation(String xmlFileLocation) 
	{
		if (xmlFileLocation == "" || xmlFileLocation == null)
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; Cant set xmlFileLocation to nothing");
		}
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
			log.write(false, "[ERROR] Network-EncodeDecodeXml; Could not delete file at: " + xmlFileLocation);
			return false;
		}
		
		try
		{
			xmlFile.createNewFile();			
			PrintWriter xmlFileWriter = new PrintWriter(xmlFileLocation);
			xmlFileWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<" + bodyName + ">\n</" + bodyName + ">");
			xmlFileWriter.close();
			log.write(true, "[SUCCESS] Network-EncodeDecodeXml; Created new xml at: " + xmlFileLocation);
			return true;
		}
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
			return false;
		}
		catch (IOException e)
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
			return false;
		}
	}
	
	//Simple add methods
	public void addRasPiId (String id)
	{
		addOrUpdateStringInXml(id, rasPiId);
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
	public void addServerIp (String ip) 
	{
		addOrUpdateStringInXml(ip, serverIp);
	}
	public void addServerName (String name) 
	{
		addOrUpdateStringInXml(name, serverName);
	}
	public void addServerFolder (String folder) 
	{
		addOrUpdateStringInXml(folder, serverFolder);
	}
	public void addServerPassword (String password) 
	{
		addOrUpdateStringInXml(password, serverPassword);
	}
	
	
	//Simple Read methods
	public String readRasPiId ()
	{
		return readDataWithinTag(rasPiId);
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
	public String readServerIp ()
	{
		return readDataWithinTag(serverIp);
	}
	public String readServerName ()
	{
		return readDataWithinTag(serverName);
	}
	public String readServerFolder ()
	{
		return readDataWithinTag(serverFolder);
	}
	public String readServerPassword ()
	{
		return readDataWithinTag(serverPassword);
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
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " +  inputFile + " is not an existing file"); 
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
				log.write(false, "[ERROR] Network-EncodeDecodeXml; Negativ offset " + fileIndex + 
						", maybe the file at \"" + xmlFileLocation + "\" as not been created corecctly"); 
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
			log.write(true, "[SUCCESS] Network-EncodeDecodeXml; Encoded image at: \"" + 
										inputFile + "\" to xmlfile at: " + xmlFileLocation); 
			rXmlFile.close();
			bInputStream.close();
		} 
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage()); 
			return;
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage()); 
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
			String readData = readDataWithinTag("image");
			if (readData == "error" || readData == "")
			{
				log.write(false, "[ERROR] Network-EncodeDecodeXml; Cant read image tag from file \"" + xmlFileLocation + "\"");
				return;
			}
			byte[] decodedImage = DatatypeConverter.parseBase64Binary(readData);
			
			BufferedOutputStream outputStream =
							new BufferedOutputStream(new FileOutputStream(outputFileLocation));
			outputStream.write(decodedImage);
			outputStream.close();
			log.write(true, "[SUCCESS] Network-EncodeDecodeXml; Decoded image in xmlfile: \"" + 
					xmlFileLocation + "\" to image at: " + outputFileLocation); 
		} 
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage()); 
			return;
		} 
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
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
				log.write(false, "[ERROR] Network-EncodeDecodeXml; Negativ offset " + fileIndex + 
						", maybe the file at \"" + xmlFileLocation + "\" as not been created corecctly"); 
				xmlFile.delete();
				rXmlFile.close();
				return;
			}
			rXmlFile.seek(fileIndex);
			rXmlFile.writeBytes("\t<" + tagName + ">" + tagData + "</" + tagName + ">\n");
			rXmlFile.writeBytes("</" + bodyName + ">");
			rXmlFile.close();
			if (tagData.length() > lengthBeforeNoticImage) // only here so the log-file wont be spammed 
			{											   //with long strings that represent the image data in base64
				tagData = "image base64 encoding";
			}
			log.write(true, "[SUCCESS] Network-EncodeDecodeXml; Add data: \"" + tagData + 
						"\" in tag: \"" + tagName + "\" to xmlfile at: " + xmlFileLocation); 
		} 
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage()); 
			return;
		}
		catch (IOException e)
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage()); 
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
				log.write(false, "[ERROR] Network-EncodeDecodeXml; " + xmlFileLocation + " is not an existing file");
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
				log.write(false, "[ERROR] Network-EncodeDecodeXml; Couldn't delete file at: " + xmlFileLocation); 
				return;
			}
			
			if (!tmpFile.renameTo(originalFile))
			{
				log.write(false, "[ERROR] Network-EncodeDecodeXml; Couldn't rename file at: " + tmpFile.getAbsolutePath()); 
			}
		} 
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
			return;
		}
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
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
				log.write(false, "[ERROR] Network-EncodeDecodeXml; " + xmlFileLocation + " is not an existing file");
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
					String tagData;
					String tagDataPrint;
					if (tagName == serverPassword || tagName == "image") //only here so the password dosent show in the log
					{
						tagData = line.substring(tagName.length() + 3, (line.length() - tagName.length() - 3));
						tagDataPrint = "****";
					}
					else 
					{
						tagData = line.substring(tagName.length() + 3, (line.length() - tagName.length() - 3));
						tagDataPrint = tagData;
					}
					//The number 3 for the first part is the characters \t, < and >. The second
					//number 3 is for the characters <, /, and >.
					log.write(true, "[SUCCESS] Network-EncodeDecodeXml; Read data: \"" + tagDataPrint + 
							"\" from tag: \"" + tagName + "\" from xmlfile at: " + xmlFileLocation); 
					return tagData;
				}
			}
			br.close();
			log.write(false, "[WARNING] Network-EncodeDecodeXml; The tag: \"" + tagName + 
								"\" dosen't exists in xmlfile at: " + xmlFileLocation);
			return "";
		} 
		catch (FileNotFoundException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
			return "error";
		}
		catch (IOException e) 
		{
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + e.getMessage());
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
			log.write(false, "[ERROR] Network-EncodeDecodeXml; " + number + "Cant convert from long to int");
			throw new IllegalArgumentException (number + "Cant convert from long to int");
		}
		return (int) number;
	}	
}
