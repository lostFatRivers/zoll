package com.zoll.collector.listener;

/**
 * 上报数据监听者;
 * 
 * @author qianhang
 * 
 * @date 2015年9月16日 下午12:03:59
 * 
 * @project zoll-collector
 * 
 */
public interface IEventListener {

	/**
	 * 接到消息;
	 * 
	 * @param event
	 */
	public void onEvent(ReportEvent event);
}
