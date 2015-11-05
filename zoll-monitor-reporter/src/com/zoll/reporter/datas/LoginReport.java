package com.zoll.reporter.datas;

public class LoginReport extends BaseReportData {

	private int playerId;
	private String platform;
	private int serverId;

	public LoginReport(int playerId, String platform, int serverId, String dataType, int count, int countType) {
		super(dataType, count, countType);
		this.playerId = playerId;
		this.platform = platform;
		this.serverId = serverId;
	}

	@Override
	public String getParams() {
		return String.format("dataType=%s&playerId=%s&platform=%s&serverId=%s&count=%s&countType=%s", dataType, playerId, platform, serverId, count, countType);
	}

}
