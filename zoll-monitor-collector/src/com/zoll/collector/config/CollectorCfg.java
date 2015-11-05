package com.zoll.collector.config;

/**
 * Collector配置;
 * 
 * @author qianhang
 * 
 * @date 2015年9月16日 下午12:22:14
 * 
 * @project zoll-collector
 * 
 */
public class CollectorCfg {

	/** 作为累加值增长 */
	public static final int INCREASE_COUNT = 1;
	/** 作为最新值刷新  */
	public static final int LATEST_COUNT = 2;

	public static final String DB_HOST = "jdbc:mysql://localhost:3306/bangzi?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true";

	public static final String DB_USER =  "root";

	public static final String DB_PWD =  "";
	
	/** 数据库连接池最小个数 */
	public static final int CONNECTION_POOL_MIN_SIZE = 3;
	/** 数据库连接池最大个数 */
	public static final int CONNECTION_POOL_MAX_SIZE = 6;
	
	/** 连接为有效状态 */
	public static final int CONNECTION_AVAILABLE = 0;
	/** 连接为无效状态(被其他线程获取) */
	public static final int CONNECTION_NOT_AVAILABLE = 1;
	
	/** HTTP服务端线程数量 */
	public static final int HTTP_SERVER_THREAD_SIZE = 6;

	/** DB线程I/O操作的间歇时间 */
	public static final long DB_TICK_TIME = 300;
}
