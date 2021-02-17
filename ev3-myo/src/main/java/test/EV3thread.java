package test;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

import lejos.hardware.BrickFinder;
import lejos.hardware.Sound;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

public class EV3thread extends Thread {
	ReentrantLock lock;
	int input;
	RMIRegulatedMotor motorB;
	RMIRegulatedMotor motorC;
	RMIRegulatedMotor motorA;
	int speed;

	EV3thread() throws RemoteException, MalformedURLException, NotBoundException {
		RemoteEV3 ev3 = new RemoteEV3(BrickFinder.discover()[0].getIPAddress());
		ev3.setDefault();
		Sound.beep();
		motorB = ev3.createRegulatedMotor("B", 'L');
		motorC = ev3.createRegulatedMotor("C", 'L');
		motorA = ev3.createRegulatedMotor("A", 'M');
		speed = 420;
		motorB.setSpeed(speed);
		motorC.setSpeed(speed);
		motorA.setSpeed((int) motorA.getMaxSpeed());

		lock = new ReentrantLock();
		input = -1;
	}

	public void setIndex(int index) {
		lock.lock();
		input = index;
		lock.unlock();
	}

	public void run() {
		while (true) {
			lock.lock();
			int chosenIndex = input;
			lock.unlock();
			// Stopwatch stopwatch = Stopwatch.createUnstarted();
			// stopwatch.start();
			// handle EV3 motors
			if (chosenIndex == 0) {
				// extension => rotate robot right
				try {
					motorB.forward();
					motorC.backward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 1) {
				// flexion => rotate robot left
				try {
					motorB.backward();
					motorC.forward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 7) {
				// fist => move robot forwards
				try {
					motorB.forward();
					motorC.forward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 6) {
				// palm out => move robot backwards
				try {
					motorB.backward();
					motorC.backward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 8) {
				// hibernation => stop the motors
				try {
					motorB.stop(true);
					motorC.stop(true);
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 4) {
				// pronation => hammer hit
				try {
					motorA.forward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			} else if (chosenIndex == 5) {
				// supinaton => hammer retreat
				try {
					motorA.backward();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			}
			// stopwatch.stop();
			// System.out.println(stopwatch.elapsed(TimeUnit.MILLISECONDS));
		}
	}

	void quit() {
		try {
			motorB.stop(true);
			motorC.stop(true);
			motorA.stop(true);
			motorB.close();
			motorC.close();
			motorA.close();
		} catch (RemoteException ex) {
			ex.printStackTrace();
		}
	}
}
