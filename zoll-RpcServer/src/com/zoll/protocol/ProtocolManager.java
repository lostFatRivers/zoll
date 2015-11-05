package com.zoll.protocol;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class ProtocolManager {
	private static ProtocolManager instance = new ProtocolManager();
	
	private static Map<Integer, Parser<? extends Message>> protobufMap = new HashMap<Integer, Parser<? extends Message>>();
	
	private ProtocolManager() {
		
	}
	
	public static ProtocolManager getInstance() {
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parserFrom(int type, byte[] datas) throws InvalidProtocolBufferException {
		Parser<? extends Message> parser = protobufMap.get(type);
		return (T) parser.parseFrom(datas);
	}
	
	public byte[] registMessage(int type, Message protol) {
		if (!protobufMap.containsKey(type)) {
			protobufMap.put(type, protol.getParserForType());
		}
		return protol.toByteArray();
	}
}
