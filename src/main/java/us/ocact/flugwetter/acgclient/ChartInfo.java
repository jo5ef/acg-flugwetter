package us.ocact.flugwetter.acgclient;

import java.util.Date;
import java.util.regex.Pattern;

public class ChartInfo {
	private Pattern pattern;
	private String pagePath;
	private String chartPathFormatString;
	
	public ChartInfo(String pagePath, String pattern, String chartPathFormatString) {
		this.pagePath = pagePath;
		this.pattern = Pattern.compile(pattern);
		this.chartPathFormatString = chartPathFormatString;
	}
	
	public String getPagePath() {
		return pagePath;
	}
	
	public String getChartPath(Date timestamp) {
		return String.format(chartPathFormatString, timestamp.getTime() / 1000);
	}
	
	public Pattern getPattern() {
		return pattern;
	}
}