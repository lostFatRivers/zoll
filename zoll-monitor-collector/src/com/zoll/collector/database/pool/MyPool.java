package com.zoll.collector.database.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zoll.collector.config.CollectorCfg;

public class MyPool implements IConnectionPool{
	private Map<Connection, Integer> connections = new HashMap<Connection, Integer>();

	@Override
	public void initPool() {
		synchronized (connections) {
			for (int i = 0; i < CollectorCfg.CONNECTION_POOL_MAX_SIZE; i++) {
				try {
					Connection connection = DriverManager.getConnection(CollectorCfg.DB_HOST, CollectorCfg.DB_USER, CollectorCfg.DB_PWD);
					connections.put(connection, CollectorCfg.CONNECTION_AVAILABLE);
				} catch (SQLException e) {
					System.err.println("DBManager connet fail E:" + e.getMessage());
				}
			}
		}
	}

	/**
	 * 获得数据库连接；
	 * 
	 * @return
	 */
	@Override
	public Connection getConnection() {
		synchronized (connections) {
			try {
				Collection<Integer> values = connections.values();
				while (!values.contains(CollectorCfg.CONNECTION_AVAILABLE)) {
					connections.wait();
				}
				Set<Entry<Connection, Integer>> entrySet = connections.entrySet();
				for (Entry<Connection, Integer> entry : entrySet) {
					if (entry.getValue() == CollectorCfg.CONNECTION_AVAILABLE) {
						entry.setValue(CollectorCfg.CONNECTION_NOT_AVAILABLE);
						return entry.getKey();
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		throw new RuntimeException("can't get Connection!");
	}

	/**
	 * 关闭连接(将连接交还给连接池, 并未真正关闭)
	 * 
	 * @param conn
	 */
	@Override
	public void closeConnection(Connection conn) {
		synchronized (connections) {
			Set<Entry<Connection, Integer>> entrySet = connections.entrySet();
			for (Entry<Connection, Integer> entry : entrySet) {
				if (entry.getKey() == conn) {
					entry.setValue(CollectorCfg.CONNECTION_AVAILABLE);
					connections.notifyAll();
					return;
				}
			}
			throw new RuntimeException("not include this connection!");
		}
	}

	/**
	 * 关闭所有连接;
	 */
	@Override
	public void closePool() {
		Set<Entry<Connection, Integer>> entrySet = connections.entrySet();
		for (Entry<Connection, Integer> entry : entrySet) {
			Connection connection = entry.getKey();
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
