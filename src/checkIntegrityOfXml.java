import java.io.File;


/**
 * Checks the integrity of a xml file. Only checks if the tag and data are 
 * there. I also check if the file has a proper size.
 * @author P. Andersson
 */
public class checkIntegrityOfXml 
{
	private Log log;
	private String filePath;
	private Long xmlLectureNoteSize = 33000000L; //Change here the target XML size 33,000,000 == 33MB
	
	
	public checkIntegrityOfXml (Log log, String filePath)
	{
		this.log = log;
		this.filePath = filePath;
	}
	
	/**
	 * Checks the integrity of a lecture note xml file
	 * @return True if file is fine. Otherwise false
	 */
	public boolean checkLecturNote ()
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(filePath, log);
		File xmlFile = new File(filePath);
		
		if (xmlFile.length() < xmlLectureNoteSize)
		{
			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; File to be check is not above " +
					"the target size. File: " + filePath);
			return false;
		}
		
		//Checks if the errors arises when trying to read data from tags
		if (xmlEditor.readRasPiId().equals("error") ||
			xmlEditor.readLectureHall().equals("error") || 
			xmlEditor.readCourseCode().equals("error") || 
			xmlEditor.readLectureName().equals("error") || 
			xmlEditor.readLectureTime().equals("error")
			)
		{
			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; Can't read data from file: " + filePath);
			return false;
		}
		
		//Checks if the XML tags exists and contains data
		if (xmlEditor.readRasPiId().equals("") ||
			xmlEditor.readLectureHall().equals("") || 
			xmlEditor.readCourseCode().equals("") || 
			xmlEditor.readLectureName().equals("") || 
			xmlEditor.readLectureTime().equals("")
			)
		{
			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; Can't find all the tags or " +
					"it is missing data within tags for a lecture note from file: " + filePath);
			return false;
		}
		
		log.write(true, "[SUCESS] Network-CheckIntegrityOfXml; Nothing wrong with the lecture note file: " + filePath);
		return true;
	}
	
	public boolean checkConfig ()
	{
		EncodeDecodeXml xmlEditor = new EncodeDecodeXml(filePath, log);
		File xmlFile = new File(filePath);
		
//		if (xmlFile.length() < xmlTargetSize) ///TODO FIX THIS
//		{
//			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; File to be check is not above " +
//					"the target size. File: " + filePath);
//			return false;
//		}
		
		//Checks if the errors arises when trying to read data from tags
		if (xmlEditor.readRasPiId().equals("error") ||
			xmlEditor.readLectureHall().equals("error") || 
			xmlEditor.readRasPiIpAddress().equals("error") || 
			xmlEditor.readRasPiPassword().equals("error")
			)
		{
			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; Can't read data from file: " + filePath);
			return false;
		}
		
		//Checks if the XML tags exists and contains data
		if (xmlEditor.readRasPiId().equals("") ||
			xmlEditor.readLectureHall().equals("") || 
			xmlEditor.readRasPiIpAddress().equals("") || 
			xmlEditor.readRasPiPassword().equals("") 
			)
		{
			log.write(false, "[ERROR] Network-CheckIntegrityOfXml; Can't find all the tags or " +
					"it is missing data within tags for a config from file: " + filePath);
			return false;
		}
		
		log.write(true, "[SUCESS] Network-CheckIntegrityOfXml; Nothing wrong with the config file: " + filePath);
		return true;
	}
}
