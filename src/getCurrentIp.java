import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Get the current ip address for the computer. Uses linux commands so
 * dont use this class when not on a linux computer
 * 
 * @author P. Andersson
 *
 */
public class getCurrentIp
{
	/**
	 * Gets the current ip address for this machine
	 * @param ethernetName The name for the connection to chech the ip. Like etho
	 * @return Returns the ip-address for the machine as a string
	 */
	public String get (String ethernetName)
	{
		try {
			NetworkInterface ni = NetworkInterface.getByName(ethernetName);
			Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
	
			while(inetAddresses.hasMoreElements()) {
				InetAddress ia = inetAddresses.nextElement();
				if(!ia.isLinkLocalAddress()) {
					return ia.getHostAddress();
				}
			}
			return "";
		}
		catch (SocketException e)
		{
			return "";
		}
	}
}