package com.zoll.view;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class ButtonD1 extends JFrame {
	private JButton
		b1 = new JButton("开始"),
		b2 = new JButton("结束");
	public ButtonD1() {
		setLayout(new FlowLayout());
		add(b1);
		add(b2);
	}
	
	public static void main(String[] args) {
		SwingConsole.run(new ButtonD1());
	}
}
