package com.zoll.reporter;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.zoll.reporter.datas.LoginReport;
import com.zoll.reporter.datas.OnlineReport;

public class ReportMain {
	public static void main(String[] args) {
		TReporter tr = new TReporter();
		tr.init("/statsd", "http://localhost:9099", 10000);
		
		ExecutorService exec = Executors.newCachedThreadPool();
		
		for (int i = 0; i < 50; i++) {
			exec.execute(new Reeeep(tr));
		}
		
	}
}

class Reeeep implements Runnable {
	private final TReporter tr;
	
	public Reeeep(TReporter tr) {
		this.tr = tr;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				tr.report(new LoginReport(new Random().nextInt(10000), "android", new Random().nextInt(240), "Login", 1, 1));
				tr.report(new OnlineReport("Online", new Random().nextInt(10000), 2, "ios", new Random().nextInt(240)));
				TimeUnit.MILLISECONDS.sleep(new Random().nextInt(10));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
