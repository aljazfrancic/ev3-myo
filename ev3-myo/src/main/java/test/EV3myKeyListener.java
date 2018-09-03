package test;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class EV3myKeyListener extends KeyAdapter {
	EV3thread ev3thread;

	EV3myKeyListener(EV3thread ev3thr) {
		ev3thread = ev3thr;
	};

	public void keyPressed(KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_UP) {
			ev3thread.setIndex(3);
		} else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
			ev3thread.setIndex(7);
		} else if (evt.getKeyCode() == KeyEvent.VK_LEFT) {
			ev3thread.setIndex(1);
		} else if (evt.getKeyCode() == KeyEvent.VK_RIGHT) {
			ev3thread.setIndex(0);
		} else if (evt.getKeyCode() == KeyEvent.VK_Q) {
			ev3thread.setIndex(4);
		} else if (evt.getKeyCode() == KeyEvent.VK_W) {
			ev3thread.setIndex(5);
		}
	}

	public void keyReleased(KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
				|| evt.getKeyCode() == KeyEvent.VK_LEFT || evt.getKeyCode() == KeyEvent.VK_RIGHT) {
			ev3thread.setIndex(8);
		}
	}
}
