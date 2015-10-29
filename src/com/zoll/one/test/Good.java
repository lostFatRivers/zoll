package com.zoll.one.test;

public class Good {
	private static int count = 0;
	private final static int id = count++;
	
	public static int getId() {
		return id;
	}
}
