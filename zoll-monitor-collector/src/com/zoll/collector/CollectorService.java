package com.zoll.collector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.httpclient.HttpStatus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zoll.collector.config.CollectorCfg;

public class CollectorService {
	private HttpServer httpServer;
	
	public void init(String address, int port, int param) {
		try {
			httpServer = HttpServer.create(new InetSocketAddress(address, port), param);
			httpServer.setExecutor(Executors.newFixedThreadPool(CollectorCfg.HTTP_SERVER_THREAD_SIZE));
			httpServer.createContext("/statsd", new DefaultCollectorHandler());
			httpServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Map<String, String> getParams(HttpExchange httpExchange) {
		Map<String, String> paramMap = new HashMap<String, String>();
		@SuppressWarnings("unused")
		String path = httpExchange.getRequestURI().getPath();
		String query = httpExchange.getRequestURI().getQuery();
		if (query != null && query.split("&").length != 0) {
			String[] sp1 = query.split("&");
			for (String eacheSp1 : sp1) {
				String[] sp2 = eacheSp1.split("=");
				if (sp2.length == 2) {
					paramMap.put(sp2[0], sp2[1]);
				}
			}
		}
		return paramMap;
	}

	public static void response(HttpExchange httpExchange, String response) {
		if (response != null && response.length() > 0) {
			try {
				byte[] bytes = response.getBytes("UTF-8");
				httpExchange.sendResponseHeaders(HttpStatus.SC_OK, bytes.length);
				httpExchange.getResponseBody().write(bytes);
				httpExchange.getResponseBody().close();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}
