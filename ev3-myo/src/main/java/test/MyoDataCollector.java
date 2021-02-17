package test;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JLabel;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Quaternion;

public class MyoDataCollector extends AbstractDeviceListener {
	ArrayList<ArrayList<byte[]>> emgSamples;
	ArrayList<ArrayList<int[]>> rmsSamples;
	ArrayList<ArrayList<double[]>> orientationSamples;
	ArrayList<int[]> drawEmgScaled;
	ArrayList<int[]> drawRmsScaled;
	ArrayList<int[]> drawOrientationScaled;
	ReentrantLock myoLock;
	ArrayList<JFGEMG> jfgsEmg;
	ArrayList<JFGOrie> jfgsOrie;
	int samplesToDraw;
	int absMaxValEmg;
	int halfHeight;
	int rmsSampleNumber;
	MultiLayerNetwork neuralNetworkModel;
	JLabel nnLabel;
	String[] nnMeanings;
	EV3thread ev3thread;

	public MyoDataCollector(ArrayList<ArrayList<byte[]>> samplesEmg, ArrayList<ArrayList<int[]>> samplesRms,
			ArrayList<ArrayList<double[]>> samplesOrie, ArrayList<int[]> emgDraw, ArrayList<int[]> rmsEmgDraw,
			ArrayList<int[]> orieDraw, ReentrantLock lockMyo, ArrayList<JFGEMG> graphicElementsEmg,
			ArrayList<JFGOrie> graphicElementsOrie, int sampToDraw, int rmsSampNum, MultiLayerNetwork model,
			JLabel nnLab, EV3thread ev3thr) {
		emgSamples = samplesEmg;
		rmsSamples = samplesRms;
		orientationSamples = samplesOrie;
		drawEmgScaled = emgDraw;
		drawRmsScaled = rmsEmgDraw;
		drawOrientationScaled = orieDraw;
		myoLock = lockMyo;
		jfgsEmg = graphicElementsEmg;
		jfgsOrie = graphicElementsOrie;
		samplesToDraw = sampToDraw;
		absMaxValEmg = 1;
		halfHeight = jfgsEmg.get(0).getHeight() / 2;
		rmsSampleNumber = rmsSampNum;
		neuralNetworkModel = model;
		nnLabel = nnLab;
		nnMeanings = new String[] { "extension", "flexion", "rad dev", "ul dev", "pron", "sup", "palm out", "fist",
				"hibernation" };
		ev3thread = ev3thr;
	}

	public void onEmgData(Myo myo, long timestamp, byte[] emg) {
		myoLock.lock();

		// add newest samples
		emgSamples.get(0).add(emg);

		// calculate RMS
		int[] rms = new int[8];
		int size = emgSamples.get(0).size() < rmsSampleNumber ? emgSamples.get(0).size() : rmsSampleNumber;
		ArrayList<byte[]> samples = new ArrayList<byte[]>(
				emgSamples.get(0).subList(emgSamples.get(0).size() - size, emgSamples.get(0).size()));
		for (int i = 0; i < 8; i++) {
			for (int ii = 0; ii < size; ii++) {
				// multiplication is faster than Math.pow
				rms[i] += samples.get(ii)[i] * samples.get(ii)[i];
			}
			rms[i] /= size;
			rms[i] = (int) Math.sqrt(rms[i]);
		}
		rmsSamples.get(0).add(rms);

		// check for absolute maximum value
		for (byte sample : emg) {
			int absSample = Math.abs(sample);
			if (absSample > absMaxValEmg) {
				absMaxValEmg = absSample;
			}
		}

		// create ArrayList of samples for drawing
		ArrayList<byte[]> drawSamples = emgSamples.get(0).size() <= samplesToDraw
				? new ArrayList<byte[]>(emgSamples.get(0))
				: new ArrayList<byte[]>(
						emgSamples.get(0).subList(emgSamples.get(0).size() - samplesToDraw, emgSamples.get(0).size()));
		ArrayList<int[]> drawAverage = rmsSamples.get(0).size() <= samplesToDraw
				? new ArrayList<int[]>(rmsSamples.get(0))
				: new ArrayList<int[]>(
						rmsSamples.get(0).subList(rmsSamples.get(0).size() - samplesToDraw, rmsSamples.get(0).size()));

		// transform to graph scaled values
		drawEmgScaled.clear();
		drawRmsScaled.clear();
		for (int i = 0; i < drawSamples.size(); i++) {
			int[] b = new int[8];
			int[] a = new int[8];
			for (int ii = 0; ii < 8; ii++) {
				b[ii] = ((int) ((((double) drawSamples.get(i)[ii]) / absMaxValEmg) * halfHeight)) + halfHeight;
				a[ii] = ((int) ((((double) drawAverage.get(i)[ii]) / absMaxValEmg) * halfHeight)) + halfHeight;
			}
			drawEmgScaled.add(b);
			drawRmsScaled.add(a);
		}

		useNeuralNetwork();

		myoLock.unlock();

		// call the repaint method on emg jfgs
		for (int i = 0; i < jfgsEmg.size(); i++) {
			jfgsEmg.get(i).repaint();
		}

	}

	// IMU real event
	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
		Quaternion normalized = rotation.normalized();

		double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()),
				1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
		double pitch = Math
				.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
		double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()),
				1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

		onImu(new double[] { roll, pitch, yaw });

	}

	// IMU fake and real event callee
	public void onImu(double[] values) {
		myoLock.lock();

		// padding loop (fill with values until it's the same size as emg)
		while (emgSamples.get(0).size() > orientationSamples.get(0).size()) {
			orientationSamples.get(0).add(values);
		}

		// create ArrayList of samples for drawing
		ArrayList<double[]> drawSamples = orientationSamples.get(0).size() <= samplesToDraw
				? new ArrayList<double[]>(orientationSamples.get(0))
				: new ArrayList<double[]>(orientationSamples.get(0)
						.subList(orientationSamples.get(0).size() - samplesToDraw, orientationSamples.get(0).size()));

		// transform to graph scaled values
		drawOrientationScaled.clear();
		for (int i = 0; i < drawSamples.size(); i++) {
			int[] b = new int[3];
			for (int ii = 0; ii < 3; ii++) {
				b[ii] = (int) (((drawSamples.get(i)[ii] / Math.PI) * halfHeight) + halfHeight);
			}
			drawOrientationScaled.add(b);
		}

		useNeuralNetwork();

		myoLock.unlock();

		// call the repaint method on orientation jfgs
		for (int i = 0; i < jfgsOrie.size(); i++) {
			jfgsOrie.get(i).repaint();
		}
	}

	private void useNeuralNetwork() {
		// NEURAL NETWORK LOGIC
		if (neuralNetworkModel != null && rmsSamples.get(0).size() > 0 && orientationSamples.get(0).size() > 0) {
			int[] rms = rmsSamples.get(0).get(rmsSamples.get(0).size() - 1);

			// scale the RMS values to [0, 1]
			double[] scaledInputs = new double[rms.length];
			for (int i = 0; i < rms.length; i++) {
				scaledInputs[i] = ((double) rms[i]) / 128.0;
			}

			if (neuralNetworkModel.layerInputSize(0) == 9) {
				double[] orie = orientationSamples.get(0).get(orientationSamples.get(0).size() - 1);
				double rollScaled = orie[0] / (Math.PI * 2) + 0.5;
				double[] newInputs = new double[rms.length + 1];
				System.arraycopy(scaledInputs, 0, newInputs, 0, scaledInputs.length);
				newInputs[newInputs.length - 1] = rollScaled;
				scaledInputs = newInputs;
			}

			// calculate neural network outputs
		    int[] shape = new int[]{1, scaledInputs.length};
			INDArray indaInputs = Nd4j.create(scaledInputs, shape);
			INDArray indaOutputs = neuralNetworkModel.output(indaInputs);
			double[] outputs = indaOutputs.toDoubleVector();

			// find index of max output value
			int maxIndex = 0;
			for (int i = 1; i < outputs.length; i++) {
				if (outputs[i] > outputs[maxIndex]) {
					maxIndex = i;
				}
			}
			
			//minimal activity (rms channel sum) threshold to avoid unwanted activations
			int sum = 0;
			for (int i = 0; i < rms.length; i++) {
				sum += rms[i];
			}
			
			if (sum < 90){
				maxIndex = 8;
			}
			
			if (ev3thread != null) {
				ev3thread.setIndex(maxIndex);
			}

			// set label
			nnLabel.setText(nnMeanings[maxIndex]);
		}
	}

}
