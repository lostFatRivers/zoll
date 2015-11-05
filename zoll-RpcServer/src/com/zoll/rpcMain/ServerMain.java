package com.zoll.rpcMain;

import com.zoll.invok.ProtocolInvok;
import com.zoll.server.RpcServer;

public class ServerMain {
	public static void main(String[] args) {
		ProtocolInvok.getInstance().init();
		RpcServer.getInstance().initServer();
	}
}
