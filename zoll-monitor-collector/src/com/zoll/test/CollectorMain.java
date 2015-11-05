package com.zoll.test;

import java.util.concurrent.TimeUnit;

import com.zoll.collector.CollectorService;
import com.zoll.collector.database.DBManager;

public class CollectorMain {
	public static void main(String[] args) {
		CollectorService cs = new CollectorService();
		cs.init("0.0.0.0", 9099, 100);
		while (true) {
			try {
				TimeUnit.SECONDS.sleep(100);
				DBManager.getInstance().checkConnection();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
}
