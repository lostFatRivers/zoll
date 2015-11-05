package com.zoll.server.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.zoll.protocol.Protocol;

public class RpcEncoder extends ProtocolEncoderAdapter {

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		if (message instanceof Protocol) {
			Protocol protocol = (Protocol) message;
			IoBuffer buffer = protocol.doEncode();
			out.write(buffer);
		}
	}

}
