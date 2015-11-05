package com.zoll.collector;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zoll.collector.listener.ReportEvent;
import com.zoll.collector.listener.ReportEventBus;

public class DefaultCollectorHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		Map<String, String> params = CollectorService.getParams(httpExchange);
		String dataType = params.get("dataType");
		ReportEvent event = new ReportEvent(dataType);
		event.setDatas(params);
		ReportEventBus.fireReportEvent(event);
		CollectorService.response(httpExchange, "\"status\":\"Finished\"");
	}

}
