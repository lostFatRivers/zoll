package com.zoll.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.google.protobuf.AbstractMessage.Builder;
import com.zoll.anno.MessagePT;
import com.zoll.anno.RPCStub;
import com.zoll.client.RpcClientBus;
import com.zoll.protocol.ParamType;
import com.zoll.protocol.Protocol;
import com.zoll.protocol.message.RpcMessage.Request;

public class CallRemoteHandler implements InvocationHandler {

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		if (supportCall(method)) {

			Protocol protocol = createProtocol(proxy, method, args);

			Object callBack = RpcClientBus.getInstance().call(protocol);

			Object result = getResult(callBack, args);
			
			return result;

		} else {
			// 如果没有加注解的话, 说明该方法并非RPC接口使用到的方法, 也就没必要让其继续执行
			// 1. 抛出异常
			// 2. 返回String
			throw new UnsupportedOperationException(proxy.getClass().getName() + "[" +this.toString()+ "]");
		}

	}

	/**
	 * 是否有注解;
	 * 
	 * @param method
	 * @return
	 */
	private boolean supportCall(Method method) {
		MessagePT annotype = method.getAnnotation(MessagePT.class);
		if (annotype == null) {
			return false;
		}
		return true;
	}

	/**
	 * 组装协议;
	 * 
	 * @param proxy
	 * @param method
	 * @param args
	 * @return
	 */
	private Protocol createProtocol(Object proxy, Method method, Object[] args) {
		Class<?>[] clazzs = proxy.getClass().getInterfaces();
		String clazzName = "";
		for (Class<?> class1 : clazzs) {
			RPCStub annotation = class1.getAnnotation(RPCStub.class);
			if (annotation != null) {
				clazzName = class1.getName();
				break;
			}
		}

		if ("".equals(clazzName)) {
			throw new RuntimeException("The Interface has not RPCStub annotation");
		}

		MessagePT annotype = method.getAnnotation(MessagePT.class);

		Builder<?> paramInfo = annotype.paramType().getParseMessage().compressReqeust(args);

		Request.Builder response = Request.newBuilder();
		response.setClazzName(clazzName);
		response.setMethodName(method.getName());
		response.setParams(paramInfo.build().toByteString());
		response.setParamType(annotype.paramType().getId());

		Protocol protocol = new Protocol(1, response);

		return protocol;
	}

	/**
	 * 解析返回参数;
	 * 
	 * @param callBack
	 * @param args
	 * @return
	 */
	private Object getResult(Object callBack, Object[] args) {
		if (callBack instanceof Protocol) {
			Request result = ((Protocol) callBack).parseProtocol(Request.getDefaultInstance());

			Object response = ParamType.valueOf(result.getParamType()).getParseMessage().parseResultFromResponse(result, args);

			return response;
		}
		return null;
	}

}
