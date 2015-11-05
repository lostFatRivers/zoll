package com.zoll.db;

import java.util.HashMap;
import java.util.Map;

public enum CountType {
	INCREASE_COUNT(1) {
		@Override
		public String getFormat() {
			return "select sum(count) from %s where createTime>'%s' and createTime<'%s'";
		}
	},
	LATEST_COUNT(2) {
		@Override
		public String getFormat() {
			return "select count from %s where createTime>'%s' and createTime<'%s' order by createTime desc limit 1";
		}
	},
	;
	
	private int id;
	
	private static Map<Integer, CountType> types = new HashMap<Integer, CountType>();
	
	static {
		for (CountType eacheType : CountType.values()) {
			if (!types.containsKey(eacheType.getId())) {
				types.put(eacheType.getId(), eacheType);
			}
		}
	}
	
	private CountType(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public static CountType valueOf(int type) {
		if (types.containsKey(type)) {
			return types.get(type);
		}
		return null;
	}
	
	public abstract String getFormat();
}
