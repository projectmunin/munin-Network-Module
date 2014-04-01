import javax.script.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.*;
import java.io.*;
 
public class JavaJavaScript {

	public static void main(String args[]) throws ScriptException, IOException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");

		String room = "EF";

		//Get data-id attribute from div#objectbasketitemX0 on this url (replace EF with room)
		//https://se.timeedit.net/web/chalmers/db1/public/objects.html?max=15&fr=t&partajax=t&im=f&sid=3&l=en_US&search_text=EF&types=186
		String id = "192402.186";

		//Get URL as string
		//https://se.timeedit.net/static/3_5_3_P_0955/min.js
		//Strip all lines except lines 9585-9737
		//Eval it. replace the line below.
		engine.eval(readFile("/Users/Simon/Desktop/javatest/min.js", StandardCharsets.UTF_8));

		engine.eval(readFile("/Users/Simon/Desktop/javatest/extra.js", StandardCharsets.UTF_8));
		engine.eval("var urls = ['https://se.timeedit.net/web/chalmers/db1/public/ri.html', 'h=t&sid=3&p=0.m%2C20140630.x'];");
		engine.eval("var keyValues = ['h=t', 'sid=3', 'p=0.m%2C20140630.x', 'objects=" + id + "', 'ox=0', 'types=0', 'fe=0'];");
		engine.eval("var url = TEScramble.asURL(urls, keyValues);");
		String url = (String)engine.get("url");

		System.out.println(url);
	}

	public static String readFile(String path, Charset encoding) 
	throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
}