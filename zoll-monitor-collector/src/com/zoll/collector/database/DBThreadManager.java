package com.zoll.collector.database;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.zoll.collector.config.CollectorCfg;

/**
 * DB线程管理器;
 * 
 * @author qianhang
 * 
 * @date 2015年9月22日 下午5:14:55
 * 
 * @project zoll-monitor-collector
 * 
 */
public class DBThreadManager {
	/** 计数器 */
	public static AtomicLong countAtomicLong = new AtomicLong(0);

	private ExecutorService exec;
	/** 拥有的DB线程 */
	private CopyOnWriteArrayList<MysqlPerformer> threadList;

	public void initExecutor() {
		if (exec == null) {
			exec = Executors.newFixedThreadPool(CollectorCfg.HTTP_SERVER_THREAD_SIZE);
			threadList = new CopyOnWriteArrayList<MysqlPerformer>();
			for (int i = 0; i < CollectorCfg.HTTP_SERVER_THREAD_SIZE; i++) {
				MysqlPerformer dbTask = new MysqlPerformer();
				threadList.add(dbTask);
				exec.execute(dbTask);
			}
		}
	}

	/**
	 * 根据计数器余数来决定分配给哪个线程;
	 * 
	 * @param sql
	 * @param tableName
	 */
	public void dispatchSql(String sql, String tableName) {
		MysqlPerformer dbTask = threadList.get((int) Math.abs(countAtomicLong.get() % CollectorCfg.HTTP_SERVER_THREAD_SIZE));
		dbTask.appendSql(sql, tableName);
	}

	private void increaseCount() {
		// if (countAtomicLong.get() == 10001) {
		// System.exit(0);
		// }
		System.out.println(countAtomicLong.addAndGet(1));
	}

	/**
	 * 
	 * @author qianhang
	 * 
	 * @date 2015年9月22日 下午5:18:22
	 * 
	 * @project zoll-monitor-collector
	 * 
	 */
	class MysqlPerformer implements Runnable {
		private Map<String, StringBuilder> sbs = new HashMap<String, StringBuilder>();
		private long tick = 0;

		public MysqlPerformer() {
		}

		/**
		 * 追加sql语句;
		 * 
		 * @param sql
		 * @param name
		 */
		public void appendSql(String sql, String name) {
			synchronized (sbs) {
				if (sbs.containsKey(name)) {
					sbs.get(name).append("," + sql.split("values")[1]);
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append(sql);
					sbs.put(name, sb);
				}
			}
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				if (new Date().getTime() - tick > CollectorCfg.DB_TICK_TIME) {
					DBManager.getInstance().checkConnection();
					synchronized (sbs) {
						if (sbs.size() > 0) {
							Collection<StringBuilder> values = sbs.values();
							for (StringBuilder eacheSb : values) {
								DBManager.getInstance().executeSql(eacheSb.toString());
							}
							sbs.clear();
							increaseCount();
						}
					}
					tick = new Date().getTime();
				} else {
					try {
						TimeUnit.MILLISECONDS.sleep(30);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
