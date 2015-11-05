package com.zoll.junit;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class SomeTest {

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		boolean flag = false;
		if (flag) {
			fail("Not yet implemented");
		}
	}
	
	@Test
	public void test2() {
		System.out.println(-321321 % 4);
	}
	
	@Test
	public void test3() {
		AtomicLong count = new AtomicLong(0);
		System.out.println(count.addAndGet(Long.MAX_VALUE));
		System.out.println(count.addAndGet(12));
	}

	@Test
	public void test4() {
		String str = "insert into test (name)values('fds'),('fds'),('fds'),('fds')";
		String[] split = str.split("values");
		for (String string : split) {
			System.out.println(string);
		}
	}
}
