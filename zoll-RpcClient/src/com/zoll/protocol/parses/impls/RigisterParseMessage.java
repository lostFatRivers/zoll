package com.zoll.protocol.parses.impls;

import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.zoll.business.IReportService.RegisterData;
import com.zoll.protocol.ParamType;
import com.zoll.protocol.Protocol;
import com.zoll.protocol.message.RpcMessage.RegisterDataInfo;
import com.zoll.protocol.message.RpcMessage.Request;
import com.zoll.protocol.parses.IParseMessage;

public class RigisterParseMessage implements IParseMessage {

	@Override
	public Builder<?> compressReqeust(Object[] params) {
		if (params.length != 1) {
			throw new RuntimeException("Param error");
		}
		RegisterData registerData = (RegisterData) params[0];
		RegisterDataInfo.Builder builder = RegisterDataInfo.newBuilder();
		builder.setPuid(registerData.getPuid()).setDevice(registerData.getDevice()).setPlayerId(registerData.getPlayerId()).setTime(registerData.getTime());
		return builder;
	}

	@Override
	public Object[] parseParamFromProto(Message proto) {
		if (proto instanceof RegisterDataInfo) {
			RegisterDataInfo param = (RegisterDataInfo) proto;
			RegisterData data = new RegisterData(param.getPuid(), param.getDevice(), param.getPlayerId(), param.getTime());
			return new Object[] { data };
		}
		return null;
	}

	@Override
	public Builder<?> compressResponse(Object[] params, Object result) {
		if (params.length != 1) {
			throw new RuntimeException("Param error");
		}
		RegisterData paramData = (RegisterData) params[0];
		Builder<?> paramInfo = RegisterDataInfo.newBuilder().setPuid(paramData.getPuid()).setDevice(paramData.getDevice()).setPlayerId(paramData.getPlayerId()).setTime(paramData.getTime());

		Request.Builder builder = Request.newBuilder().setParams(ByteString.copyFrom(paramInfo.build().toByteArray())).setParamType(ParamType.REGISTER_DATA.getId());

		return builder;
	}

	@Override
	public Object parseResultFromResponse(Request result, Object[] args) {
		RegisterDataInfo registerDataInfo = (RegisterDataInfo) Protocol.parseProtocol(ParamType.REGISTER_DATA.getDefaultInstance(), result.getParams().toByteArray());
		if (args.length != 1) {
			throw new RuntimeException("Param error");
		}

		RegisterData paramData = (RegisterData) args[0];
		paramData.setPuid(registerDataInfo.getPuid());
		paramData.setDevice(registerDataInfo.getDevice());
		paramData.setPlayerId(registerDataInfo.getPlayerId());
		paramData.setTime(registerDataInfo.getTime());

		return null;
	}

}
