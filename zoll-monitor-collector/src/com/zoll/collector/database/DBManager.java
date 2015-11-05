package com.zoll.collector.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;

import com.zoll.collector.database.pool.HikariPool;
import com.zoll.collector.database.pool.IConnectionPool;
import com.zoll.collector.listener.ReportEvent;

/**
 * 持久化数据管理器;
 * 
 * @author qianhang
 * 
 * @date 2015年9月21日 下午2:48:29
 * 
 * @project zoll-monitor-collector
 * 
 */
public class DBManager {
	private static DBManager instance = new DBManager();
	/** 数据库连接池 */
	private IConnectionPool connectionPool;

	/** DB线程管理器 */
	private DBThreadManager tm = new DBThreadManager();

	private DBManager() {
		checkConnection();
		tm.initExecutor();
	}

	public void checkConnection() {
		if (connectionPool == null) {
			// 使用 HikariCP 来作为数据库连接池
			connectionPool = new HikariPool();
			connectionPool.initPool();
		}
	}

	public static DBManager getInstance() {
		return instance;
	}

	/**
	 * 查询数据库中是否有该表;
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasTable(String name) {
		ResultSet tables = null;
		try {
			checkConnection();
			DatabaseMetaData metaData = connectionPool.getConnection().getMetaData();
			tables = metaData.getTables(null, null, name, null);
			if (tables != null && tables.next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 插入一条数据;
	 * 
	 * @param datas
	 * @param eventName
	 */
	public void insertData(Map<String, String> datas, String eventName) {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into " + eventName + " (");
		Iterator<String> iterator = datas.keySet().iterator();
		while (iterator.hasNext()) {
			String column = iterator.next();
			sb.append(column + ",");
		}
		sb.append("createTime)values(");
		Iterator<String> iterator2 = datas.keySet().iterator();
		while (iterator2.hasNext()) {
			String column = iterator2.next();
			sb.append("'" + datas.get(column) + "',");
		}
		sb.append("'" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "')");
		// 提交给ThreadManager分配执行;
		tm.dispatchSql(sb.toString(), eventName);
	}

	/**
	 * 创建数据库表;
	 * 
	 * @param event
	 */
	public void createTable(ReportEvent event) {
		StringBuilder sb = new StringBuilder();
		String tableName = event.getDataType();
		Iterator<String> iterator = event.getDatas().keySet().iterator();
		sb.append("CREATE TABLE " + tableName + " (id int auto_increment primary key,");
		while (iterator.hasNext()) {
			String column = iterator.next();
			sb.append(column + " varchar(25)");
			sb.append(",");
		}
		sb.append("`createTime` timestamp NOT NULL DEFAULT '" + DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "')");
		executeSql(sb.toString());
	}

	/**
	 * 执行sql;
	 * 
	 * @param sql
	 */
	public void executeSql(String sql) {
		try {
			System.out.println(sql);
			Connection connection = connectionPool.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();
			statement.close();
			connectionPool.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
