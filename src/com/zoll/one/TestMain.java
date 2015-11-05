package com.zoll.one;

import java.util.HashMap;
import java.util.Map;

public class TestMain {
	public static void main(String[] args) {
		Integer in = getNext();
		if (in == null) {
			System.out.println("ddssdds");
		}
	}
	
	public static int getNext() {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		return map.get(1);
	}
}
