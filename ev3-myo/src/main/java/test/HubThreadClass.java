package test;

import com.thalmic.myo.Hub;

public class HubThreadClass extends Thread {
	Hub hub;

	public HubThreadClass(Hub h) {
		hub = h;
	}

	public void run() {
		hub.run(0);
	}

}
