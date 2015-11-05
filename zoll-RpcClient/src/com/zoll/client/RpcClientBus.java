package com.zoll.client;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.zoll.client.codec.RpcDecoder;
import com.zoll.client.codec.RpcEncoder;
import com.zoll.client.handler.MyHanlder;
import com.zoll.protocol.Protocol;
import com.zoll.tools.NetConfig;

/**
 * RPC事件总线;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 下午2:17:30
 * 
 * @project zoll-RpcClient
 * 
 */
public class RpcClientBus {
	private static RpcClientBus instance = new RpcClientBus();

	private IoConnector connector;
	private IoSession session;

	private RpcClientBus() {

	}

	public static RpcClientBus getInstance() {
		return instance;
	}

	/**
	 * 初始化Client;
	 */
	public void initClient() {
		connector = new NioSocketConnector();
		connector.getSessionConfig().setUseReadOperation(true);
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolMyFactory(new RpcEncoder(), new RpcDecoder())));
		connector.setConnectTimeoutMillis(NetConfig.CONNECT_TIME_OUT);
		connector.setHandler(new MyHanlder());
		ConnectFuture connectF = connector.connect(new InetSocketAddress(NetConfig.SERVER_HOST, NetConfig.SERVER_PORT));
		while (!connectF.isConnected()) {
			continue;
		}
		session = connectF.getSession();
		System.out.println("connect success");
	}

	/**
	 * 阻塞的远程调用接口;
	 * 
	 * @param protocol
	 * @return
	 */
	public Object call(Protocol protocol) {
		if (session == null || !session.isConnected()) {
			throw new RuntimeException("not already connect server");
		}
		WriteFuture write = session.write(protocol);
		write.awaitUninterruptibly();
		if (write.getException() != null) {
			System.err.println(write.getException().getMessage());
		}

		ReadFuture readF = session.read();
		readF.awaitUninterruptibly();
		if (readF.getException() != null) {
			System.err.println(write.getException().getMessage());
		} else {
			Object response = readF.getMessage();
			if (response instanceof Protocol) {
				Protocol responseProtocol = (Protocol) response;
				return responseProtocol;
			}
		}
		return null;
	}

}
