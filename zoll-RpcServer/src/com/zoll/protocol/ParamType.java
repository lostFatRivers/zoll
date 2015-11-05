package com.zoll.protocol;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;
import com.zoll.protocol.message.RpcMessage.LoginDataInfo;
import com.zoll.protocol.message.RpcMessage.RegisterDataInfo;
import com.zoll.protocol.parses.IParseMessage;
import com.zoll.protocol.parses.impls.RigisterParseMessage;

/**
 * 参数类型枚举; 
 * 
 * @optimize 是否应该将返回值类型拆出, 用另外一个枚举; 但是返回的对象中也会包含参数;
 * 
 * @author qianhang
 * 
 * @date 2015年8月25日 上午11:30:48
 * 
 * @project zoll-RpcClient
 * 
 */
public enum ParamType {
	REGISTER_DATA(1) {
		@Override
		public Message getDefaultInstance() {
			return RegisterDataInfo.getDefaultInstance();
		}

		@Override
		public IParseMessage getParseMessage() {
			return new RigisterParseMessage();
		}

	},

	LOGIN_DATA(2) {

		@Override
		public Message getDefaultInstance() {
			return LoginDataInfo.getDefaultInstance();
		}

		@Override
		public IParseMessage getParseMessage() {
			// TODO Auto-generated method stub
			return null;
		}

	},

	GOLD_DATA(3) {
		@Override
		public Message getDefaultInstance() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IParseMessage getParseMessage() {
			// TODO Auto-generated method stub
			return null;
		}

	},

	SERVER_DATA(4) {
		@Override
		public Message getDefaultInstance() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public IParseMessage getParseMessage() {
			// TODO Auto-generated method stub
			return null;
		}


	},

	;

	private static Map<Integer, ParamType> typeMap = new HashMap<Integer, ParamType>();
	protected int id;

	static {
		for (ParamType eacheType : ParamType.values()) {
			if (typeMap.containsKey(eacheType.getId())) {
				System.err.println("Has other id : " + typeMap.get(eacheType.getId()).name() + " <<<>>> " + eacheType.name());
				continue;
			} else {
				typeMap.put(eacheType.getId(), eacheType);
			}
		}
	}

	ParamType(int id) {
		this.id = id;
	}

	/**
	 * 获得默认对象, 用于反序列化;
	 * 
	 * @return
	 */
	public abstract Message getDefaultInstance();

	public abstract IParseMessage getParseMessage();

	public int getId() {
		return this.id;
	}

	public static ParamType valueOf(int id) {
		return typeMap.get(id);
	}
}
