package us.ocact.flugwetter.acgclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class FlugwetterClient {

	private static String baseUrl = "https://www.austrocontrol.at/flugwetter";
	private CloseableHttpClient httpClient;
	private HttpClientContext ctx;
	private CookieStore cookieStore;
	private boolean loggedIn;
	
	private static Map<Charts, ChartInfo> chartInfos = new EnumMap<Charts, ChartInfo>(Charts.class);
	
	static {
		chartInfos.put(Charts.AlpforNeu, new ChartInfo("/index.php?id=442", "alpfor-neu.gif\\?mtime=(\\d+)", "/products/chartloop/alpfor-neu.gif?mtime=%d"));
		chartInfos.put(Charts.AlpforUpd, new ChartInfo("/index.php?id=442", "alpfor-upd.gif\\?mtime=(\\d+)", "/products/chartloop/alpfor-upd.gif?mtime=%d"));
		chartInfos.put(Charts.AlpforPdf, new ChartInfo("/index.php?id=442", "alpfor.pdf\\?mtime=(\\d+)", "/products/chartloop/alpfor-pdf.gif?mtime=%d"));
		chartInfos.put(Charts.Gafor, new ChartInfo("/index.php?id=446", "gafor.gif\\?mtime=(\\d+)", "/products/chartloop/gafor.gif?mtime=%d"));
		chartInfos.put(Charts.LlwSwcNeu, new ChartInfo("/index.php?id=442", "llswc-neu.gif\\?mtime=(\\d+)", "/products/chartloop/llswc-neu.gif?mtime=%d"));
		chartInfos.put(Charts.LlwSwcUpd, new ChartInfo("/index.php?id=442", "llswc-upd.gif\\?mtime=(\\d+)", "/products/chartloop/llswc-upd.gif?mtime=%d"));
		chartInfos.put(Charts.LlwSwcPdf, new ChartInfo("/index.php?id=442", "llswc.pdf\\?mtime=(\\d+)", "/products/chartloop/llswc.pdf?mtime=%d"));
	}
	
	public FlugwetterClient() {
		cookieStore = new BasicCookieStore();
		httpClient = HttpClients.custom()
			.setDefaultCookieStore(cookieStore)
			.build();
		
		ctx = HttpClientContext.create();
		ctx.setCookieStore(cookieStore);
	}
	
	public boolean login(String user, String password) throws ClientProtocolException, IOException {
		
		if(loggedIn) {
			throw new IllegalStateException("already logged in");
		}
		
		get("/index.php");	// initial request to retrieve cookies
		
		HttpPost post = new HttpPost(baseUrl + "/acglogin.cgi");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", user));
		nvps.add(new BasicNameValuePair("password", password));
		nvps.add(new BasicNameValuePair("back", baseUrl + "/index.php"));
		post.setEntity(new UrlEncodedFormEntity(nvps));
		CloseableHttpResponse response = httpClient.execute(post, ctx);
		
		if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			// verify cookies/response?
			loggedIn = true;
		}
		
		return loggedIn;
	}
	
	public byte[] downloadFile(Charts chart, Date timestamp) throws IOException {
		ChartInfo ci = chartInfos.get(chart);
		HttpGet get = new HttpGet(baseUrl + ci.getChartPath(timestamp));
		CloseableHttpResponse response = httpClient.execute(get, ctx);
		return EntityUtils.toByteArray(response.getEntity());
	}
	
	public String get(String path) throws ClientProtocolException, IOException {
		HttpGet get = new HttpGet(baseUrl + path);
		CloseableHttpResponse response = httpClient.execute(get, ctx);
		return EntityUtils.toString(response.getEntity());
	}
	
	public Map<Charts, Date> getTimestamps(Charts charts[]) throws IOException {
		
		Map<Charts, Date> res = new HashMap<Charts, Date>();
		
		for(Charts c : charts) {
			ChartInfo ci = chartInfos.get(c);
			String page = get(ci.getPagePath());
			Matcher m = ci.getPattern().matcher(page);
			if(m.find()) {
				res.put(c, new Date(Long.parseLong(m.group(1)) * 1000));
			}
		}
		
		return res;
	}
}