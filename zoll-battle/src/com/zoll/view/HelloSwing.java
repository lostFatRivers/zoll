package com.zoll.view;

import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class HelloSwing {
	public static void main(String[] args) throws InterruptedException {
		showLable();
	}
	
	public static void showSwing() {
		JFrame frame = new JFrame("Hello Swing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 200);
		frame.setVisible(true);
	}
	
	public static void showLable() throws InterruptedException {
		JFrame frame = new JFrame("Hello Swing");
		JLabel label = new JLabel("touch me");
		frame.add(label);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300, 200);
		frame.setVisible(true);
		TimeUnit.SECONDS.sleep(1);
		label.setText("why ?");
	}
	
	public static void submitTask() {
		
	}
}
