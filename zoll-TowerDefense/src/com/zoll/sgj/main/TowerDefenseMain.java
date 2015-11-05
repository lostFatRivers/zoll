package com.zoll.sgj.main;

import com.zoll.sgj.map.node.Point;
import com.zoll.sgj.map.road.Way;
import com.zoll.sgj.tools.range.Ranger;

public class TowerDefenseMain {
	public static void main(String[] args) {
		Point a = new Point(0, 0);
		Point b = new Point(300, 800);
		
		System.out.println(Ranger.rangeOf(a, b));
		
		Point w1 = new Point(5, 12);
		Point w2 = new Point(5, 2);
		Point w3 = new Point(3, 2);
		Point w4 = new Point(3, 18);
		Point w5 = new Point(9, 18);
		Point w6 = new Point(9, 5);
		Point w7 = new Point(18, 5);
		Point w8 = new Point(18, 8);
		
		Way way = new Way(w1,w2,w3,w4,w5,w6,w7,w8);
		way.painWay();
	}
}