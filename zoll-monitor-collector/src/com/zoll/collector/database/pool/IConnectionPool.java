package com.zoll.collector.database.pool;

import java.sql.Connection;

public interface IConnectionPool {
	public void initPool();
	
	public Connection getConnection();
	
	public void closeConnection(Connection conn);
	
	public void closePool();
}
