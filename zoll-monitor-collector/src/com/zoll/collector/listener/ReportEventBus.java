package com.zoll.collector.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReportEventBus {
	private static Map<String, List<IEventListener>> registers = new ConcurrentHashMap<String, List<IEventListener>>();
	
	public static void fireReportEvent(ReportEvent event) {
		if (registers.containsKey(event.getDataType())) {
			List<IEventListener> list = registers.get(event.getDataType());
			for (IEventListener eacheListener : list) {
				eacheListener.onEvent(event);
			}
		} else {
			List<IEventListener> list = new CopyOnWriteArrayList<IEventListener>();
			list.add(new ReportEventListener(event));
			registers.put(event.getDataType(), list);
		}
	}
	
	public IEventListener getEventListener(String name, Class<?> clazz) {
		List<IEventListener> list = registers.get(name);
		for (IEventListener eacheListener : list) {
			if (eacheListener.getClass().getName().equals(clazz.getName())) {
				return eacheListener;
			}
		}
		return null;
	}
}
