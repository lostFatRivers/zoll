package com.zoll.client;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import com.zoll.tools.DefaultNumberCodecs;
import com.zoll.tools.NumberCodec;

/**
 * 编解码器工厂;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 上午11:33:31
 * 
 * @project zoll-RpcClient
 * 
 */
public class ProtocolMyFactory implements ProtocolCodecFactory {

	public static NumberCodec numberCodec = DefaultNumberCodecs.getBigEndianNumberCodec();

	private ProtocolEncoder rpcEncoder;
	private ProtocolDecoder rpcDecoder;

	public ProtocolMyFactory(ProtocolEncoder rpcEncoder, ProtocolDecoder rpcDecoder) {
		this.rpcEncoder = rpcEncoder;
		this.rpcDecoder = rpcDecoder;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		return rpcEncoder;
	}

	@Override
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		return rpcDecoder;
	}

}
