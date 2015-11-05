package com.zoll.sgj.tools.range;

import com.zoll.sgj.map.node.Point;

/**
 * 求距离工具类;
 * 
 * @author qianhang
 * 
 * @date 2015年8月29日 下午4:49:59
 * 
 * @project zoll-TowerDefense
 * 
 */
public class Ranger {

	/**
	 * 求两点之间的距离;
	 * 
	 * @param begin
	 * @param end
	 * @return
	 */
	public static int rangeOf(Point begin, Point end) {
		int range_x = Math.abs(begin.getCoords_x() - end.getCoords_x());
		int range_y = Math.abs(begin.getCoords_y() - end.getCoords_y());
		return (int) Math.sqrt(Math.pow(range_x, 2) + Math.pow(range_y, 2));
	}
}
