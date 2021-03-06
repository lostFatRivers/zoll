package com.zoll.view;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SwingConsole {
	public static void run(final JFrame jf, final int width, final int height) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				jf.setTitle(jf.getClass().getSimpleName());
				jf.setSize(width, height);
				jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				jf.setVisible(true);
			}
			
		});
	}
	
	public static void run(final JFrame jf) {
		run(jf, 300, 200);
	}
}
