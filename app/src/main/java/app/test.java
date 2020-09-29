package app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import app.MyMonitor.MyHandler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import comm.HttpResponse;
import comm.RestHttpClient;

public class test {
	private static String originator = "admin:admin";
	private static String cseProtocol = "http";
	private static String cseIp = "127.0.0.1";
	private static int csePort = 8080;
	private static String cseId = "in-cse";
	private static String cseName = "in-name";

	private static String aeMonitorName = "mymonitor";
	private static String aeProtocol = "http";
	private static String aeIp = "127.0.0.1";
	private static int aePort = 1601;
	private static String subName = "monitorsub";
	private static String targetCse = "mn-cse-3/mn-name-3/MONITOR/DATA";

	private static String csePoa = cseProtocol + "://" + cseIp + ":" + csePort;
	private static String appPoa = aeProtocol + "://" + aeIp + ":" + aePort;

	private static JSONObject ae;
	private static JSONObject sub;

	public static void main(String[] args) {

		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(aePort), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.createContext("/", new MyHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

		JSONArray array = new JSONArray();
		array.put(appPoa);
		JSONObject obj = new JSONObject();
		obj.put("rn", aeMonitorName);
		obj.put("api", 12346);
		obj.put("rr", true);
		obj.put("poa", array);
		ae = new JSONObject();
		ae.put("m2m:ae", obj);
		RestHttpClient.post(originator, csePoa + "/~/" + cseId + "/" + cseName,
				ae.toString(), 2);

		array = new JSONArray();
		array.put("/" + cseId + "/" + cseName + "/" + aeMonitorName);
		obj = new JSONObject();
		obj.put("nu", array);
		obj.put("rn", subName);
		obj.put("nct", 2);
		sub = new JSONObject();
		sub.put("m2m:sub", obj);
		RestHttpClient.post(originator, csePoa + "/~/" + targetCse,
				sub.toString(), 23);

		/*
		 * 
		 * for(int i=0;i<9;i++){ HttpResponse httpResponse = RestHttpClient.get(
		 * originator, csePoa + "/~/" + targetCse + "/" + "LORAGW" + "?rcn=" +
		 * i); } HttpResponse httpResponse = RestHttpClient.get( originator,
		 * csePoa + "/~/" + targetCse + "/" + "LORAGW" + "?rcn=5&lvl=1");
		 */
	}

	static class MyHandler implements HttpHandler {

		public void handle(HttpExchange httpExchange) throws IOException {
			System.out.println("Event Recieved!");
			
			InputStream in = httpExchange.getRequestBody();
			String requestBody = "";
			int i;
			char c;
			while ((i = in.read()) != -1) {
				c = (char) i;
				requestBody = (String) (requestBody + c);
			}
			System.out.println(requestBody);
			
			
			String responseBudy ="";
			byte[] out = responseBudy.getBytes("UTF-8");
			httpExchange.sendResponseHeaders(200, out.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(out);
			os.close();
			
		}
		
		
	}
}
