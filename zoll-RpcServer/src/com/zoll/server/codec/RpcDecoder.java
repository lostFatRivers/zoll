package com.zoll.server.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.zoll.protocol.Protocol;
import com.zoll.tools.DefaultNumberCodecs;
import com.zoll.tools.NetConfig;
import com.zoll.tools.NumberCodec;

public class RpcDecoder extends CumulativeProtocolDecoder {

	private NumberCodec numberCodec= DefaultNumberCodecs.getBigEndianNumberCodec();
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		int type = 0;
		int length = 0;
		
		Object typeAttr = session.getAttribute(NetConfig.TYPE_SESSION_ATTRIBUTE);
		Object lengthAttr = session.getAttribute(NetConfig.LENGTH_SESSION_ATTRIBUTE);
		
		// 1. while can't read type and length from session attribute	===> read from IoBuffer				
		if (typeAttr == null || lengthAttr == null) {
			// 1.1. while in.remaining() less than head length  			===> continue to accept head
			if (in.remaining() < NetConfig.PROTOCOL_HEAD_LENGTH) {
				return false;
			}
			// 1.2. while can read type and length							===> read type and length,then put in session
			type = getIntFromIoBuffer(in, NetConfig.INT_BUFFER_SIZE);
			length = getIntFromIoBuffer(in, NetConfig.INT_BUFFER_SIZE);
			session.setAttribute(NetConfig.TYPE_SESSION_ATTRIBUTE, type);
			session.setAttribute(NetConfig.LENGTH_SESSION_ATTRIBUTE, length);
		} else {
			// 2. can get type and length from session					===> read from session
			type = (int) typeAttr;
			length = (int) lengthAttr;
			
		}
		
		// 3. can read last data from IoBuffer							===> get data body and write to Output
		if (in.remaining() >= length) {
			byte[] datas = new byte[length];
			in.get(datas);
			System.out.print("Datas length : " + datas.length + "\n");
			for (byte b : datas) {
				System.out.print(b);
			}
			System.out.println();
			Protocol protocol =new Protocol(type, length, datas);
			
			session.removeAttribute(NetConfig.TYPE_SESSION_ATTRIBUTE);
			session.removeAttribute(NetConfig.LENGTH_SESSION_ATTRIBUTE);
			
			out.write(protocol);
			
			return true;
		}
		
		// 4. not enough to get data body								===> continue to accept body
		return false;
	}
	
	private int getIntFromIoBuffer(IoBuffer in, int byteLength) {
		byte[] typeByte = new byte[byteLength];
		in.get(typeByte);
		return numberCodec.bytes2Int(typeByte, byteLength);
	}

}
