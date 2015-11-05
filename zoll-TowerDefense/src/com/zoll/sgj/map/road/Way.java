package com.zoll.sgj.map.road;

import java.util.ArrayList;
import java.util.List;

import com.zoll.sgj.map.node.Point;

public class Way {
	private List<Point> wayCorner;

	public Way(Point... corner) {
		wayCorner = new ArrayList<Point>();
		for (Point eachePoint : corner) {
			wayCorner.add(eachePoint);
		}
		if (!checkWayValid()) {
			throw new RuntimeException("The way is invalid !!");
		}
	}

	private boolean checkWayValid() {
		for (int i = 0; i < wayCorner.size(); i++) {
			if (i == wayCorner.size() - 2) {
				return true;
			}
			if (!((wayCorner.get(i).getCoords_x() == wayCorner.get(i + 1).getCoords_x()) && ((wayCorner.get(i).getCoords_y() != wayCorner.get(i + 1).getCoords_y())) || (wayCorner.get(i).getCoords_x() != wayCorner
					.get(i + 1).getCoords_x()) && ((wayCorner.get(i).getCoords_y() == wayCorner.get(i + 1).getCoords_y())))) {
				System.out.println(wayCorner.get(i) + " == " + wayCorner.get(i + 1));
				return false;
			}
		}
		return false;
	}
	
	public void painWay() {
		String[][] map = new String[20][20];
		
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 20; j++) {
				map[i][j] = "-";
			}
		}
		
		for (int i = 0; i < wayCorner.size(); i++) {
			if (i == wayCorner.size() - 1) {
				break;
			}
			List<int[]> line = getLine(wayCorner.get(i), wayCorner.get(i + 1));
			for (int[] js : line) {
				map[js[0]][js[1]] = "+";
			}
		}
		
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 20; j++) {
				System.out.print(map[i][j]);
			}
			System.out.println();
		}
	}
	
	private List<int[]> getLine(Point start, Point end) {
		List<int[]> lineList = new ArrayList<int[]>();
		if (start.getCoords_x() == end.getCoords_x()) {
			for (int i = min(start.getCoords_y(), end.getCoords_y()); i <= max(start.getCoords_y(), end.getCoords_y()); i++) {
				lineList.add(new int[]{start.getCoords_x(), i});
			}
			return lineList;
		} else if (start.getCoords_y() == end.getCoords_y()) {
			for (int i = min(start.getCoords_x(), end.getCoords_x()); i <= max(start.getCoords_x(), end.getCoords_x()); i++) {
				lineList.add(new int[]{i, start.getCoords_y()});
			}
			return lineList;
		} else {
			throw new RuntimeException("ERROR LINE");
		}
		
	}
	
	private int max(int num, int num2) {
		return (num > num2 ? num : num2);
	}
	
	private int min(int num, int num2) {
		return (num > num2 ? num2 : num);
	}
}
