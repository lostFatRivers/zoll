package com.zoll.reporter.datas;

public class OnlineReport extends BaseReportData {
	private String platform;
	private int serverId;
	
	public OnlineReport(String dataType, int count, int countType, String platform, int serverId) {
		super(dataType, count, countType);
		this.platform = platform;
		this.serverId = serverId;
	}

	@Override
	public String getParams() {
		return String.format("dataType=%s&platform=%s&serverId=%s&count=%s&countType=%s", dataType, platform, serverId, count, countType);
	}

}
