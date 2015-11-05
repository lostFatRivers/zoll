package com.zoll.business.impl;

import com.google.protobuf.ByteString;
import com.zoll.business.IReportService;
import com.zoll.client.RpcClientBus;
import com.zoll.protocol.ParamType;
import com.zoll.protocol.Protocol;
import com.zoll.protocol.message.RpcMessage.RegisterDataInfo;
import com.zoll.protocol.message.RpcMessage.Request;
import com.zoll.protocol.message.RpcMessage.Response;

public class PlayerReportService implements IReportService {

	@Override
	public void report(RegisterData registerData) {

		RegisterDataInfo.Builder registerDataInfo = RegisterDataInfo.newBuilder()
																	.setPuid(registerData.getPuid())
																	.setDevice(registerData.getDevice())
																	.setPlayerId(registerData.getPlayerId())
																	.setTime(registerData.getTime());
		Request.Builder builder = Request.newBuilder()
										.setClazzName(this.getClass().getName())
										.setMethodName("report")
										.setParamType(ParamType.REGISTER_DATA.getId())
										.setParams(ByteString.copyFrom(registerDataInfo.build().toByteArray()));

		Protocol protocol = new Protocol(1, builder);

		Object callBack = RpcClientBus.getInstance().call(protocol);
		
		if (callBack instanceof Protocol) {
			Response response = ((Protocol) callBack).parseProtocol(Response.getDefaultInstance());
				
			RegisterDataInfo parseFrom = (RegisterDataInfo) Protocol.parseProtocol(ParamType.valueOf(response.getParamType()).getDefaultInstance(), response.getParams().toByteArray());
			
			registerData.setPuid(parseFrom.getPuid());
			registerData.setDevice(parseFrom.getDevice());
			registerData.setPlayerId(parseFrom.getPlayerId());
			registerData.setTime(parseFrom.getTime());
		}
	}

	@Override
	public void report(LoginData loginData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void report(GoldData goldData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void report(ServerData serverData) {
		// TODO Auto-generated method stub

	}

}
