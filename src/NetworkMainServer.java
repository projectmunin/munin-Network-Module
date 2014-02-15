
public class NetworkMainServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		NetworkServer networkServer = new NetworkServer();
		networkServer.setPort(55353);
		networkServer.setFolderLocation("D:/test");
		networkServer.run();

	}

}
