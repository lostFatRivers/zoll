package com.zoll.reporter.datas;

public abstract class BaseReportData implements IReportData {

	protected String dataType;
	protected int count;
	protected int countType;
	
	public BaseReportData(String dataType, int count, int countType) {
		this.dataType = dataType;
		this.count = count;
		this.countType = countType;
	}
	
}
