package com.zoll.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.zoll.server.codec.RpcDecoder;
import com.zoll.server.codec.RpcEncoder;
import com.zoll.server.handler.MyHandler;
import com.zoll.tools.NetConfig;

public class RpcServer {
	private static RpcServer instance = new RpcServer();
	
	private IoAcceptor acceptor;
	
	private RpcServer() {
		
	}
	
	public static RpcServer getInstance() {
		return instance;
	}
	
	public void initServer() {
		acceptor = new NioSocketAcceptor();
		acceptor.getSessionConfig().setReadBufferSize(NetConfig.BUFFER_SIZE);
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolMyFactory(new RpcEncoder(), new RpcDecoder())));
		acceptor.setHandler(new MyHandler());
		try {
			acceptor.bind(new InetSocketAddress(NetConfig.SERVER_PORT));
		} catch (IOException e) {
			System.err.println("Server start error : " + e);
		}
		System.out.println("Server started...");
	}
	
}
