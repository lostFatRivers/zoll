package com.zoll.collector.listener;

import com.zoll.collector.database.DBManager;

public class ReportEventListener implements IEventListener {
	private String eventName;

	public ReportEventListener(ReportEvent event) {
		this.eventName = event.getDataType();
		boolean hasTable = DBManager.getInstance().hasTable(eventName);
		if (hasTable) {
			DBManager.getInstance().insertData(event.getDatas(), eventName);
		} else {
			DBManager.getInstance().createTable(event);
		}
	}

	public void onEvent(ReportEvent event) {
		this.eventName = event.getDataType();
		DBManager.getInstance().insertData(event.getDatas(), eventName);
	}
}
