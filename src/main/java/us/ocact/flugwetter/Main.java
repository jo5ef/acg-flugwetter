package us.ocact.flugwetter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import us.ocact.flugwetter.acgclient.Charts;
import us.ocact.flugwetter.acgclient.FlugwetterClient;

import com.google.gson.Gson;

public class Main {

	static DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public static void main(String[] args) {
		try {
			Charts[] charts = new Charts[] { Charts.AlpforNeu, Charts.AlpforUpd, Charts.Gafor };
			
			File prevTsFile = new File("prev-ts.json");
			Map<Charts, Date> prevTs = new HashMap<Charts, Date>();
			if(prevTsFile.exists()) {
				prevTs = load(prevTsFile);
			}
			
			FlugwetterClient client = new FlugwetterClient();
			if(!client.login(args[0], args[1])) {
				System.out.println("login failed!");
			}
			
			Map<Charts, Date> timestamps = client.getTimestamps(charts);
			
			for(Charts c : timestamps.keySet()) {
				if(prevTs.containsKey(c) && !prevTs.get(c).before(timestamps.get(c))) {
					System.out.println("skipping " + c + ", it has not changed.");
					continue;
				}
				
				byte[] data = client.downloadFile(c, timestamps.get(c)); 
				
				writeFile(c + ".gif", data);
				writeFile(c + "-" + df.format(timestamps.get(c)) + ".gif", data);
				
				prevTs.put(c, timestamps.get(c));
			}
						
			storeTimestamps(prevTsFile, prevTs);
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private static Map<Charts, Date> load(File f) throws IOException, ParseException {
		Gson gson = new Gson();
		Map<String, String> m = new HashMap<String, String>();
		m = gson.fromJson(new FileReader(f), m.getClass());
		Map<Charts, Date> res = new HashMap<Charts, Date>();
		for(String x : m.keySet()) {
			res.put(Charts.valueOf(x), df.parse(m.get(x)));
		}
		return res;
	}
	
	private static void storeTimestamps(File f, Map<Charts, Date> m) throws IOException {
		Gson gson = new Gson();
		FileWriter fw = new FileWriter(f, false);
		Map<String, String> x = new HashMap<String, String>();
		for(Charts c : m.keySet()) {
			x.put(c.name(), df.format(m.get(c)));
		}
		fw.write(gson.toJson(x));
		fw.close();
	}
	
	private static void writeFile(String fileName, byte[] data) throws IOException {
		FileOutputStream f = new FileOutputStream(fileName);
		f.write(data);
		f.close();
	}
}