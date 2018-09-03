package test;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

public class JFGEMG extends JPanel {
	static final long serialVersionUID = 1L;

	ArrayList<int[]> drawValues;
	ArrayList<int[]> drawAverages;
	ReentrantLock emgLock;
	int channel;

	public JFGEMG(ArrayList<int[]> drawVals, ArrayList<int[]> drawAvg, ReentrantLock lockEmg, int ch) {
		drawValues = drawVals;
		drawAverages = drawAvg;
		emgLock = lockEmg;
		channel = ch;
	}

	public void paint(Graphics g) {
		super.paint(g);

		emgLock.lock();

		// draw values
		g.setColor(new Color(0, 0, 0, 255));
		for (int i = 0; i < drawValues.size() - 1; i++) {
			g.drawLine(i, drawValues.get(i)[channel], i + 1, drawValues.get(i + 1)[channel]);
		}

		// draw average values
		g.setColor(new Color(255, 0, 0, 255));
		for (int i = 0; i < drawAverages.size() - 1; i++) {
			g.drawLine(i, drawAverages.get(i)[channel], i + 1, drawAverages.get(i + 1)[channel]);
		}

		emgLock.unlock();

	}
}
