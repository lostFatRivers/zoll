package junitTest;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.zoll.anno.RPCStub;
import com.zoll.business.IReportService;
import com.zoll.business.IReportService.CommonData;

public class RpcClientTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
	}
	
	@Test
	public void test2() {
		CommonData data = new CommonData("432", "fdsa", 432, "43.44.221.33");
		System.out.println(data.getClass().getName());
	}
	
	@Test
	public void test3() {
		String str = "dfsafdsfnjdklsajcidopwqr234riopfndiusa8v-c90ja8if9pd8-";
		
		byte[] bytes = str.getBytes();
		
		String stre = new String(bytes);
		
		System.out.println(stre);
	}
	
	@Test
	public void test4() {
		String str = "dfsafdsfnjdklsajcidopwqr234ras分红放倒后风挡放大子女从i后风挡年底喀什警方ido";
		
		try {
			byte[] bytes = str.getBytes("GBK");
			
			System.out.println(bytes);
			
			System.out.println(bytes.length);
			
			String stre = new String(bytes, "UTF-8");
			
			System.out.println(stre);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test5() {
		String str = "3";
		byte[] bytes = str.getBytes();
		for (byte b : bytes) {
			System.out.print(b);
		}
	}

	@Test
	public void test6() {
		String str = "dfsafdsfnjdklsajcidopwqr234ras分红放倒后风挡放大子女从i后风挡年底喀什警方ido";
		byte[] bytes = str.getBytes();
		ByteString bs = ByteString.copyFrom(bytes);
		
		byte[] byteArray = bs.toByteArray();
		String newString = new String(byteArray);
		System.out.println(newString);
	}
	
	@Test
	public void test7() throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		Class<?> loadClass = RpcClientTest.class.getClassLoader().loadClass("junitTest.TestClass");
		Method[] methods = loadClass.getMethods();
		for (Method method : methods) {
			if (method.getName().equals("printSomething")) {
				method.invoke(loadClass.newInstance());
			}
		}
	}
	
	@Test
	public void test8() {
		Nummm i = new Nummm();
		i.num = 32;
		System.out.println(numAdd(i).num);
		System.out.println(i.num);
		
		String str = "34323";
		System.out.println(addString(str));
		System.out.println(str);
	}
	
	public Nummm numAdd(Nummm num) {
		num.num = 100;
		return num;
	}
	
	public String addString(String str) {
		str = "3dfdsafdsa";
		return "ff";
	}
	
	class Nummm {
		int num = 12;
	}
	
	@Test
	public void test9() {
		String str = "341243_24321";
		System.out.println(str.indexOf("_"));
	}
	
	@Test
	public void test10() {
		Class<?> clazzClass = IReportService.class;
		System.out.println(clazzClass.getAnnotation(RPCStub.class));
	}
}
