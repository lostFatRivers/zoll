package com.zoll.reporter;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zoll.reporter.datas.IReportData;

public class TReporter {
	private static int count = 1;
	
	private HttpClient httpClient;

	/** 前缀 */
	private String prefix;

	private String host;
	
	private Executor exec;

	public void init(String prefix, String host, int timeout) {
		this.prefix = prefix;
		this.host = host;
		exec = Executors.newSingleThreadExecutor(new ReporterThreadFactory());
		httpClient = new HttpClient();
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
		httpClient.getHttpConnectionManager().getParams().setSoTimeout(timeout);
	}

	public void report(final IReportData data) {
		exec.execute(new Runnable() {
			@Override
			public void run() {
				
				try {
					GetMethod getMethod = new GetMethod(host);
					getMethod.setPath(prefix);
					getMethod.setQueryString(data.getParams());
					int serverCode = httpClient.executeMethod(getMethod);
					if (serverCode != HttpStatus.SC_OK) {
						System.out.println(data.getParams() + " is reported fail.");
					} else {
						System.out.println(count++);
					}
				} catch (HttpException e) {
					e.getClass().getSimpleName();
					System.out.println(e.getMessage());
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
			
		});
	}
	
	class ReporterThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("Statsd-" + t.getName());
			return t;
		}
		
	}
	
}
