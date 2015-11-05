package com.zoll.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoadData {
	private static LoadData instance = new LoadData();
	
	public static final String ALL_TABLE_SQL = "select table_name from information_schema.tables where table_schema='bangzi'";
	public static final String SELECT_COUNT = "select countType from %s limit 1";
	
	private static final String HOST = "jdbc:mysql://localhost:3306/bangzi?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true";
	private static final String USER_NAME = "root";
	private static final String PASS_WORD = "";
	
	private Connection connection;
	
	private LoadData() {
		checkConnection();
	}
	
	private void checkConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				Class.forName("com.mysql.jdbc.Driver");
				connection = DriverManager.getConnection(HOST, USER_NAME, PASS_WORD);
			}
		} catch (SQLException e) {
			System.err.println("DBManager connet fail E:" + e.getMessage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static LoadData getInstance() {
		return instance;
	}

	public int getTimeUnitDataCount(String startTime, String endTime, String dataType) {
		Statement statement = null;
		ResultSet result = null;
		ResultSet result2 = null;
		try {
			dataType = dataType.toLowerCase();
			statement = connection.createStatement();
			String sql = String.format(SELECT_COUNT, dataType);
			System.out.println("---get TimeUnit DataCount sql : " + sql);
			result = statement.executeQuery(sql);
			int countType = 0;
			if (result.next()) {
				countType = result.getInt(1);
			}
			String format = CountType.valueOf(countType).getFormat();
			
			String dataCountSql = String.format(format, dataType, startTime, endTime);
			result2 = statement.executeQuery(dataCountSql);
			int targetCount = 0;
			if (result2.next()) {
				targetCount = result2.getInt(1);
			}
			
			return targetCount;
		} catch (SQLException e) {
			System.out.println(e.getMessage() + " LoadData.java : 69");
		} finally {
			close(statement, result, result2);
		}
		return new Random().nextInt(100);
	}

	public List<String> getAllReportType() {
		List<String> menus = new ArrayList<String>();
		Statement statement = null;
		ResultSet result = null;
		try {
			checkConnection();
			statement = connection.createStatement();
			result = statement.executeQuery(ALL_TABLE_SQL);
			while (result.next()) {
				String tableName = result.getString(1);
				menus.add(tableName);
			}
			if (menus.size() > 0) {
				return menus;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			close(statement, result);
		}
		menus.add("Login");
		menus.add("Recharge");
		menus.add("Exception");
		menus.add("Online");
		menus.add("Logout");
		menus.add("Register");
		menus.add("EquipSwallow");
		return menus;
	}
	
	private void close(Statement statement, ResultSet... result) {
		try {
			if (statement != null) {
				statement.close();
			}
			if (result != null) {
				for (ResultSet resultSet : result) {
					if (resultSet != null) {
						resultSet.close();
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
//	public static void main(String[] args) {
//		System.out.println(LoadData.getInstance().getTimeUnitDataCount("2015-07-30 14:41:12", "2015-08-03 10:20:02", "login"));
//	}
}
