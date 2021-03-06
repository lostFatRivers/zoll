package com.zoll.view;

import java.awt.Label;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class HelloSwing {
	public static void main(String[] args) throws InterruptedException {
		submitTask();
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
	
	public static void submitTask() throws InterruptedException {
		JFrame jf = new JFrame("Hello Swing");
		jf.setSize(300, 200);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final Label lab = new Label("touch me");
		jf.add(lab);
		jf.setVisible(true);
		TimeUnit.SECONDS.sleep(2);
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				lab.setText("Use SwingUtilties");
				
			}
		});
	}
}

