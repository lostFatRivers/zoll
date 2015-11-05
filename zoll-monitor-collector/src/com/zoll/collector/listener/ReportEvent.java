package com.zoll.collector.listener;

import java.util.Map;

/**
 * 上报的数据实体;
 * 
 * @author qianhang
 * 
 * @date 2015年9月16日 下午12:24:06
 * 
 * @project zoll-collector
 * 
 */
public class ReportEvent {
	/** 上报类型 */
	private String dataType;
	/** 上报内容 */
	private Map<String, String> datas;

	public ReportEvent(String dataType) {
		this.dataType = dataType;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDatas(Map<String, String> params) {
		this.datas = params;
	}

	public Map<String, String> getDatas() {
		return this.datas;
	}
}
