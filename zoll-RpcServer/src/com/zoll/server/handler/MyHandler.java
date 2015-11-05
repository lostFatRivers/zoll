package com.zoll.server.handler;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.zoll.invok.ProtocolInvok;
import com.zoll.protocol.Protocol;

public class MyHandler implements IoHandler {

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		System.out.println(cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		System.out.println("Had received message");
		if (message instanceof Protocol) {
			Protocol protocol = (Protocol) message;
			Protocol request = ProtocolInvok.getInstance().invok(protocol);
			session.write(request);
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputClosed(IoSession session) throws Exception {
		// TODO Auto-generated method stub

	}

}
