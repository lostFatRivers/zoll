package com.zoll.protocol.parses;

import com.google.protobuf.Message;
import com.google.protobuf.AbstractMessage.Builder;
import com.zoll.protocol.message.RpcMessage.Request;

/**
 * 参数解析接口;
 * 
 * @author qianhang
 * 
 * @date 2015年8月25日 下午5:38:15
 * 
 * @project zoll-RpcClient
 * 
 */
public interface IParseMessage {
	/**
	 * 组装RPC请求的Builder对象;
	 * 
	 * @param params
	 * @return
	 */
	public Builder<?> compressReqeust(Object[] params);

	/**
	 * 从Message中获得参数数组;
	 * 
	 * @param proto
	 * @return
	 */
	public Object[] parseParamFromProto(Message proto);

	/**
	 * 组装RPC返回消息Builder对象;<br/>
	 * 
	 * @param params
	 * @param result
	 * @return
	 */
	public Builder<?> compressResponse(Object[] params, Object result);

	/**
	 * 解析出 return value 和 将返回的对象参数赋值给原本的参数;
	 * <p>
	 * 1.如果该RPC方法没有返回值, 则直接返回null;
	 * </p>
	 * 2.若有返回值, 可以用result.getResult()来获取返回值的ByteString对象, 再转化成返回值;
	 * 
	 * @param result
	 * @param args
	 * @return
	 */
	public Object parseResultFromResponse(Request result, Object[] args);
}
