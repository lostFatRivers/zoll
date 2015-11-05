package com.zoll.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.zoll.protocol.ParamType;

/**
 * RPC接口方法的注解,用于动态代理自动封装参数;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 下午5:25:27
 * 
 * @project zoll-RpcClient
 * 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessagePT {

	/**
	 * 参数类型;
	 * 
	 * @return
	 */
	ParamType paramType();
}
