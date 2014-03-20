import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Get the current ip address for the computer. Uses linux commands so 
 * dont use this class when not on a linux computer
 * @author P. Andersson
 *
 */
public class getCurrentIp 
{
	public String get ()
	{
		try
		{
			Process plsof = new ProcessBuilder(new String[]{"ifconfig", "|", "grep -Eo 'inet (addr:)?([0-9]*\\.){3}[0-9]*'", "|", "grep -Eo '([0-9]*\\.){3}[0-9]*'", "|", "grep -v '127.0.0.1'"}).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(plsof.getInputStream()));
			return reader.readLine();
		} 
		catch (IOException e) 
		{
			return "";
		}
	}
}
