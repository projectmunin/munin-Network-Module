import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Checks if a file is open by another program
 * 
 * @author P. Andersson
 *
 */
public class isCompletelyWritten 
{
	/**
	 * Checks if a file has been completely written. Uses the linux program lsof. ONLY works for linux
	 * @param file The file that will be checked if it has been written completely
	 * @return true if it has been completely written, otherwise false;
	 */
	public boolean check (String filePath)
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
			return false;
		}
	}
}
