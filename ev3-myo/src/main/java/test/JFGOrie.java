package test;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

public class JFGOrie extends JPanel {
	static final long serialVersionUID = 1L;

	ArrayList<int[]> drawValues;
	ReentrantLock orieLock;
	int channel;

	public JFGOrie(ArrayList<int[]> drawVals, ReentrantLock lockOrie, int ch) {
		drawValues = drawVals;
		orieLock = lockOrie;
		channel = ch;
	}

	public void paint(Graphics g) {
		super.paint(g);

		orieLock.lock();

		// draw values
		g.setColor(new Color(0, 0, 255, 255));
		for (int i = 0; i < drawValues.size() - 1; i++) {
			g.drawLine(i, drawValues.get(i)[channel], i + 1, drawValues.get(i + 1)[channel]);
		}

		orieLock.unlock();

	}
}
