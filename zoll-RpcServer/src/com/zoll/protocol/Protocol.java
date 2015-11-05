package com.zoll.protocol;

import org.apache.mina.core.buffer.IoBuffer;

import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.zoll.tools.NetConfig;

/**
 * package for socket communitaction;
 * 
 * @author qianhang
 * 
 * @date 2015年8月21日 下午3:42:34
 * 
 * @project zoll-RpcClient
 * 
 */
public class Protocol {

	/** message type */
	private int type;
	/** data length */
	private int length;

	private Builder<?> builder;
	
	private byte[] datas;

	/**
	 * static construction;
	 * 
	 * @param type
	 * @param builder
	 * @return
	 */
	public Protocol(int type, Builder<?> builder) {
		int length = builder.build().toByteArray().length;
		setType(type);
		setLength(length);
		setBuilder(builder);
		setDatas(builder.build().toByteArray());
	}

	/**
	 * static construction;
	 * 
	 * @param type
	 * @param length
	 * @param data
	 * @return
	 */
	public Protocol(int type, int length, byte[] data) {
		if (length != data.length) {
			throw new RuntimeException("Protocol decode error");
		}
		setType(type);
		setLength(length);
		setDatas(data);
	}

	public IoBuffer doEncode() {
		IoBuffer buffer = IoBuffer.allocate(NetConfig.BUFFER_SIZE);
		buffer.putInt(getType());
		buffer.putInt(getLength());
		if (getBuilder() != null) {
			buffer.put(this.builder.build().toByteArray());
		} else {
			buffer.put(new byte[0]);
		}
		buffer.flip();
		return buffer;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Builder<?> getBuilder() {
		return builder;
	}

	protected void setBuilder(Builder<?> builder) {
		this.builder = builder;
	}

	public void setDatas(byte[] datas) {
		this.datas = datas;
	}

	public <T extends Message> T parseProtocol(T t) {
		return parseProtocol(t, datas);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Message> T parseProtocol(T t, byte[] data) {
		Parser<? extends Message> parserForType = t.getParserForType();
		Message parseFrom = null;
		try {
			parseFrom = parserForType.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			System.err.println("Parse Protocol Error!");
			e.printStackTrace();
		}
		return (T) parseFrom;
	}

}
