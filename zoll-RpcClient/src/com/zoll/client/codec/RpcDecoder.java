package com.zoll.client.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.zoll.protocol.Protocol;
import com.zoll.tools.DefaultNumberCodecs;
import com.zoll.tools.NetConfig;
import com.zoll.tools.NumberCodec;

/**
 * 解码器;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 上午11:27:21
 *
 * @project zoll-RpcClient
 *
 */
public class RpcDecoder extends CumulativeProtocolDecoder {

	private NumberCodec numberCodec= DefaultNumberCodecs.getBigEndianNumberCodec();
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		int type = 0;
		int length = 0;
		
		Object typeAttr = session.getAttribute(NetConfig.TYPE_SESSION_ATTRIBUTE);
		Object lengthAttr = session.getAttribute(NetConfig.LENGTH_SESSION_ATTRIBUTE);
		
		// 1. 当无法从session中获取type和length两个Attribute时				===>1. 从IoBuffer中读取type和length				
		if (typeAttr == null || lengthAttr == null) {
			// 1.1. 如果IoBuffer中剩余长度小于(type的长度 + length的长度)时 			===>1.1 返回false,让IoBuffer中的数据继续累积
			if (in.remaining() < NetConfig.PROTOCOL_HEAD_LENGTH) {
				return false;
			}
			// 1.2. 当条件满足时,可以从IoBuffer中读取type和length					===>1.2 读出type和length并作为Attribute放入session
			type = getIntFromIoBuffer(in, NetConfig.INT_BUFFER_SIZE);
			length = getIntFromIoBuffer(in, NetConfig.INT_BUFFER_SIZE);
			session.setAttribute(NetConfig.TYPE_SESSION_ATTRIBUTE, type);
			session.setAttribute(NetConfig.LENGTH_SESSION_ATTRIBUTE, length);
		} else {
			// 2. 可以充session中直接取到type和length						===>2 为type和length赋值
			type = (int) typeAttr;
			length = (int) lengthAttr;
			
		}
		
		// 3. IoBuffer剩余长度大于取得的length时								===>3. 从IoBuffer中读取data实体并写入output
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
		
		// 4. 剩余长度不够时												===>4. 返回false让IoBuffer中的数据累积
		return false;
	}
	
	private int getIntFromIoBuffer(IoBuffer in, int byteLength) {
		byte[] typeByte = new byte[byteLength];
		in.get(typeByte);
		return numberCodec.bytes2Int(typeByte, byteLength);
	}

}
