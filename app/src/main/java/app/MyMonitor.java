package app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import comm.HttpResponse;
import comm.RestHttpClient;

public class MyMonitor {

	private static String originator = "admin:admin";
	private static String cseProtocol = "http";
	private static String cseIp = "127.0.0.1";
	private static int csePort = 8080;
	private static String cseId = "in-cse";
	private static String cseName = "in-name";

	private static String aeMonitorName = "mymonitor";
	private static String aeProtocol = "http";
	private static String aeIp = "127.0.0.1";
	private static int aePort = 1600;
	private static String subName = "monitorsub";
	private static String targetCse = "mn-cse/mn-name";

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
	//	System.out.println("[INFO]"+ "Create AE " + aeMonitorName);
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
	//	System.out.println("\n[INFO]"+ "Sub to " + targetCse);

		RestHttpClient.post(originator, csePoa + "/~/" + targetCse,
				sub.toString(), 23);
		// WHY ????? --> notify AE create

	//	System.out.println("\n[INFO] Discover all containers in " + csePoa + "/~/" + targetCse );
		
		String parentCnt = targetCse;
		subCnt(parentCnt);
		
//		HttpResponse httpResponse = RestHttpClient.get(originator, csePoa
//				+ "/~/" + targetCse + "?fu=1&ty=3");
//		JSONObject result = new JSONObject(httpResponse.getBody());
//		/*
//		 * String[] uril = result.getString("m2m:uril").split(" ");
//		 * 
//		 * for (int i=0;i<uril.length;i++){ RestHttpClient.post(originator,
//		 * csePoa+"/~"+uril[i], sub.toString(), 23); }
//		 */
//		// Receive all uri list of container --> Slip and sent POST to sub all
//		// container
//		JSONArray uril_arr = result.getJSONArray("m2m:uril");
//		for (Object test : uril_arr) {
//			RestHttpClient.post(originator, csePoa + "/~" + test,
//					sub.toString(), 23);
//		}
		
		
	}

	static void subCnt(String parentCnt){
		HttpResponse httpResponse = RestHttpClient.get(originator, csePoa
				+ "/~/" + parentCnt + "?fu=1&ty=3");
		JSONObject result = new JSONObject(httpResponse.getBody());
		JSONArray uril_arr = result.getJSONArray("m2m:uril");
		for (Object urlCnt : uril_arr) {
	//		System.out.println("\n[doSubCnt] Sub to uri container " + urlCnt);
			RestHttpClient.post(originator, csePoa + "/~" + urlCnt,
					sub.toString(), 23);
		}
	}
	
	static String getParent(String uri, String name){
		String strParent = null;
		String regex = ".*/(?<parentName>\\w+)."+ name + ".*";
		Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(uri);
	    System.out.println(uri + "\n" + name + "\n" + regex);
	    while (matcher.find()) {
	    	strParent = matcher.group("parentName");
	    }
	  //  String group = matcher.group();
	  //  System.out.println(uri + "\n" + name + "\n" + "regex" + group);
	//	String strParent = matcher.group("parentName");
		return strParent;
	}
	
	static class MyHandler implements HttpHandler {

		public void handle(HttpExchange httpExchange) {
			//System.out.println("[Event Recieved]");

			try {
				InputStream in = httpExchange.getRequestBody();

				String requestBody = "";
				int i;
				char c;
				while ((i = in.read()) != -1) {
					c = (char) i;
					requestBody = (String) (requestBody + c);
				}

			//	System.out.println(requestBody);

				JSONObject json = new JSONObject(requestBody);
				
				
				if (json.getJSONObject("m2m:sgn").has("m2m:vrq")) {
			//		System.out.println("[Event] Confirm subscription");
				}

				else if (json.getJSONObject("m2m:sgn").getJSONObject("m2m:nev")
						.getJSONObject("m2m:rep").has("m2m:ae")) {

					// read in to ae
					JSONObject rep = json.getJSONObject("m2m:sgn")
							.getJSONObject("m2m:nev").getJSONObject("m2m:rep")
							.getJSONObject("m2m:ae");

					int ty = rep.getInt("ty");
				//	System.out.println("LOL Sure AE. Resource type: " + ty);

					if (ty == 2) {
						// ty = 2 => print info new AE
						String aeName = rep.getString("rn");
						System.out.println("\n[EVENT] New AE has been registred: "
								+ aeName);
				//		System.out.println("\n[ACTION] Wait 3 seconds");
						Thread.sleep(3000);
				//		System.out.println("\n[ACTION] Sub to container in AE " + aeName);
						String parentCnt= targetCse + "/" + aeName;
						subCnt(parentCnt);
//						HttpResponse httpResponse = RestHttpClient.get(originator, csePoa
//								+ "/~/" + targetCse + "/" + aeName + "?fu=1&ty=3");
//						JSONObject result = new JSONObject(httpResponse.getBody());
//						JSONArray uril_arr = result.getJSONArray("m2m:uril");
//						for (Object test : uril_arr) {
//							System.out.println("[INFO] Container: "
//									+ test + "\n" );
//							RestHttpClient.post(originator, csePoa + "/~" + test,
//									sub.toString(), 23);
//						}

//						HttpResponse httpResponse = RestHttpClient.get(
//								originator, csePoa + "/~/" + targetCse + "/"
//										+ aeName + "?rcn=5");
//						// rcn = 5 means ?? - result content = 5 ==> find
//						// container in ae
//						// attributes and child resource references
//
//						JSONObject ae = new JSONObject(httpResponse.getBody());
//						if (!ae.getJSONObject("m2m:ae").isNull("ch")) {
//							JSONArray aeChild = ae.getJSONObject("m2m:ae")
//									.getJSONArray("ch");
//
//							System.out.println("[INFO] AE " + aeName + " has "
//									+ aeChild.length() + " child:");
//
//							for (int j = 0; j < aeChild.length(); j++) {
//								if (aeChild.getJSONObject(j).getInt("typ") == 3) {
//									String cntName = aeChild.getJSONObject(j)
//											.getString("nm");
//									System.out.println("[INFO] Container: "
//											+ cntName + "\n" );
//									// Sub into child of ae
//									RestHttpClient
//											.post(originator, csePoa + "/~/"
//													+ targetCse + "/" + aeName
//													+ "/" + cntName,
//													sub.toString(), 23);
//
//									/*
//									System.out.println(originator + csePoa
//											+ "/~/" + targetCse + "/" + aeName
//											+ "/" + cntName + sub.toString());
//									*/
//									
//								}
//							}
//						}
					}
				}
				
				else if(json.getJSONObject("m2m:sgn").getJSONObject("m2m:nev")
						.getJSONObject("m2m:rep").has("m2m:cnt")){
					JSONObject rep = json.getJSONObject("m2m:sgn")
							.getJSONObject("m2m:nev").getJSONObject("m2m:rep")
							.getJSONObject("m2m:cnt");
					String cntName = rep.getString("rn");
					String parent = getParent(rep.getString("ol"), cntName);
					System.out.println("\n[EVENT] New CNT has been registred: "
							+ cntName + " in " + parent);
					String parentCnt = targetCse + "/" + "LORAGW/DETECTORS/" + cntName;
					
			//		System.out.println("[ACTION] Discover all containers in " + csePoa + "/~/" + parentCnt);
					subCnt(parentCnt);
				}

				else {
					JSONObject rep = json.getJSONObject("m2m:sgn")
							.getJSONObject("m2m:nev").getJSONObject("m2m:rep")
							.getJSONObject("m2m:cin");

					int ty = rep.getInt("ty");
					if (ty == 4) {
						String ciName = rep.getString("rn");
						String content = rep.getString("con");

						System.out.println("\n[EVENT] New Content Instance "
								+ ciName + " has been created");
					//	System.out.println("[INFO] Content: " + content);

					}
				}

				String responseBudy = "";
				byte[] out = responseBudy.getBytes("UTF-8");
				httpExchange.sendResponseHeaders(200, out.length);
				OutputStream os = httpExchange.getResponseBody();
				os.write(out);
				os.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}