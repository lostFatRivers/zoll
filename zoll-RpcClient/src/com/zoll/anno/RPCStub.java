package com.zoll.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 添加该注解表明该接口需要使用RPC进行调用;
 * 
 * @author qianhang
 * 
 * @date 2015年8月24日 下午5:30:20
 * 
 * @project zoll-RpcClient
 * 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCStub {
	
	/**
	 * 服务端使用该条注解来绑定实现类;
	 * 
	 * @return
	 */
	Class<?> realWorker() default Object.class;
}
