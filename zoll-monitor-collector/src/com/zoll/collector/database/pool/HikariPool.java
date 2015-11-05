package com.zoll.collector.database.pool;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zoll.collector.config.CollectorCfg;

public class HikariPool implements IConnectionPool{
	private HikariDataSource hds;
	
	@Override
	public void initPool() {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("com.mysql.jdbc.Driver");
		config.setJdbcUrl(CollectorCfg.DB_HOST + "&user=" + CollectorCfg.DB_USER + "&password=" + CollectorCfg.DB_PWD);
		config.addDataSourceProperty("cachePrepStmts", true);
		config.addDataSourceProperty("prepStmtCacheSize", 4096);
		config.addDataSourceProperty("prepStmtCacheSqlLimit", 8192);
		config.setConnectionTestQuery("SELECT 1");
		config.setAutoCommit(true);
		
		config.setMinimumIdle(CollectorCfg.CONNECTION_POOL_MIN_SIZE);
		
		config.setMaximumPoolSize(CollectorCfg.CONNECTION_POOL_MAX_SIZE);
		
		hds = new HikariDataSource(config);
	}

	@Override
	public Connection getConnection() {
		try {
			return hds.getConnection();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			hds.resumePool();
			return null;
		}
	}

	@Override
	public void closeConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void closePool() {
		hds.close();
	}
	
}
