package com.zoll.junitTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		System.out.println(Fucker.class.getName());
		Class<?> loadClass = ServerTest.class.getClassLoader().loadClass(Fucker.class.getName());
		
		Object[] objs = {"大王!!!", 323, new StringBuilder("哈哈哈哈哈...zzzzz")};
		String[] objsClassNames = {String.class.getName(), int.class.getName(), StringBuilder.class.getName()};
		
		Method[] methods = loadClass.getMethods();
		for (Method method : methods) {
			if (!method.getName().equals("getPapapa")) {
				continue;
			}
			System.out.println(method.getName());
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length != objs.length) {
				continue;
			}
			boolean isTarget = true;
			for (int i = 0; i < parameterTypes.length; i++) {
				if (!parameterTypes[i].getName().equals(objsClassNames[i])) {
					isTarget = false;
				}
			}
			if (isTarget) {
				Object invoke = method.invoke(loadClass.newInstance(), objs);
				System.out.println(invoke);
			}
		}
		System.out.println(int.class.getName());
	}

	
	@Test
	public void test1() throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException {
		System.out.println(Fucker.class.getName());
		Class<?> loadClass = ServerTest.class.getClassLoader().loadClass(Fucker.class.getName());
		
		Object[] objs = {"大王!!!", 323, new StringBuilder("哈哈哈哈哈...zzzzz")};
		
		Class<?>[] objsClazzs = {String.class, int.class, StringBuilder.class};
		
		Method method = loadClass.getMethod("getPapapa", objsClazzs);
		
		Object invoke = method.invoke(loadClass.newInstance(), objs);
		
		System.out.println(invoke);
		
		for (Object eacheObj : objs) {
			System.out.println(eacheObj);
		}
	}
	
}

class Fucker {
	
	public void getPapapa(String str, int num, StringBuilder sb) {
		System.out.println(str);
		System.out.println(num);
		System.out.println(sb.toString());
		sb.append("NEW NEW NEW NEW NEW");
	}
	
	public Integer getPapapa(String str, int num, StringBuilder sb, String name) {
		return 211321432;
	}
}