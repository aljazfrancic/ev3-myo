package test;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.enums.StreamEmgType;

public class Test {
	// IMPORTANT VARIABLE
	final static int rmsSampleNumber = 50;

	// FRAME VARIABLE
	static JFrame frame = new JFrame("Myo EV3");

	public static void main(String[] args) {
		// EV3 INIT
		final EV3thread ev3thread[] = new EV3thread[1];
		try {
			// start the EV3 thread
			ev3thread[0] = new EV3thread();
			ev3thread[0].start();

			frame.setFocusable(true);
			// add key listener - only works if frame has focus
			frame.addKeyListener(new EV3myKeyListener(ev3thread[0]));
			// on window close
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					ev3thread[0].quit();
				}
			});
		} catch (RemoteException | MalformedURLException | NotBoundException ex) {
			ex.printStackTrace();
		} finally {

			// KERAS/DL4J (NEURAL NETWORK) INIT - LOAD MODEL AND WEIGHTS
			final MultiLayerNetwork[] neuralNetworkModel = new MultiLayerNetwork[1];
			try {
				neuralNetworkModel[0] = KerasModelImport.importKerasSequentialModelAndWeights("../_models/model");
			} catch (IOException | InvalidKerasConfigurationException | UnsupportedKerasConfigurationException ex) {
				ex.printStackTrace();
			} finally {

				// LABEL DISPLAYING NEURAL NETWORK OUTPUT
				final JLabel nnOutput = new JLabel("none");

				// EMG VARIABLES
				final ArrayList<ArrayList<byte[]>> emgSamples = new ArrayList<ArrayList<byte[]>>();
				emgSamples.add(new ArrayList<byte[]>());
				final ArrayList<ArrayList<int[]>> rmsValues = new ArrayList<ArrayList<int[]>>();
				rmsValues.add(new ArrayList<int[]>());
				final ArrayList<ArrayList<double[]>> orieSamples = new ArrayList<ArrayList<double[]>>();
				orieSamples.add(new ArrayList<double[]>());
				final ReentrantLock myoLock = new ReentrantLock();

				// HTC FOR REAL EVENTS AND SCHEDULER AND TASK FOR FAKE EVENTS
				final HubThreadClass[] htc = new HubThreadClass[1];
				final ScheduledExecutorService fakeScheduler = Executors.newSingleThreadScheduledExecutor();
				final Future<?>[] fakeTask = new Future<?>[1];

				// GUI BUTTON VARS
				final JButton buttonLiveStream = new JButton("Live stream");
				final JButton buttonSave = new JButton("Save");
				final JButton buttonLoadEMG = new JButton("Load");
				final JButton buttonStopAndReset = new JButton("Stop and reset");

				// MYO START LOGIC
				final Hub hub = new Hub("com.example.emg-data-sample");
				final Myo myo = hub.waitForMyo(1000);
				final MyoDataCollector[] myoDataCollector = new MyoDataCollector[1];
				final boolean[] liveStreamEnabled = new boolean[1];
				System.out.println("Attempting to find a Myo...");
				if (myo == null) {
					buttonLiveStream.setEnabled(liveStreamEnabled[0]);
					System.out.println("Unable to find a Myo!");
				} else {
					System.out.println("Connected to a Myo armband!");
					// create and start a new thread
					htc[0] = new HubThreadClass(hub);
					htc[0].start();
					// start streaming EMG
					myo.setStreamEmg(StreamEmgType.STREAM_EMG_ENABLED);
					// is live stream enabled
					liveStreamEnabled[0] = true;
				}

				// GUI DRAWING GRAPHS
				final ArrayList<int[]> emgDraw = new ArrayList<int[]>();
				final ArrayList<int[]> emgRmsDraw = new ArrayList<int[]>();
				final ArrayList<int[]> orieDraw = new ArrayList<int[]>();

				final int width = 900;
				final int height = 1000;

				frame.getContentPane().setLayout(new GridLayout(13, 1));
				frame.setSize(width, height);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setResizable(false);

				final ArrayList<JFGEMG> jfgsEmg = new ArrayList<JFGEMG>();
				for (int i = 0; i < 8; i++) {
					jfgsEmg.add(new JFGEMG(emgDraw, emgRmsDraw, myoLock, i));
					frame.getContentPane().add(jfgsEmg.get(i));
				}

				final ArrayList<JFGOrie> jfgsOrie = new ArrayList<JFGOrie>();
				for (int i = 0; i < 3; i++) {
					jfgsOrie.add(new JFGOrie(orieDraw, myoLock, i));
					frame.getContentPane().add(jfgsOrie.get(i));
				}

				// STOP AND RESET BUTTON LOGIC
				buttonStopAndReset.setEnabled(false);
				buttonStopAndReset.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						// cancel real events
						if (myoDataCollector[0] != null) {
							hub.removeListener(myoDataCollector[0]);
							myoDataCollector[0] = null;
						}

						// cancel fake events
						if (fakeTask[0] != null) {
							fakeTask[0].cancel(false);
						}

						// stop the motors
						if (ev3thread[0] != null) {
							ev3thread[0].setIndex(8);
						}

						// clear the buffer
						myoLock.lock();

						emgSamples.get(0).clear();
						rmsValues.get(0).clear();
						orieSamples.get(0).clear();

						myoLock.unlock();

						// handle buttons
						buttonLiveStream.setEnabled(liveStreamEnabled[0]);
						buttonLoadEMG.setEnabled(true);
						buttonSave.setEnabled(false);
						buttonStopAndReset.setEnabled(false);

					}
				});

				// LIVE STREAM BUTTON LOGIC
				buttonLiveStream.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						// MYO ADD LISTENER
						myoDataCollector[0] = new MyoDataCollector(emgSamples, rmsValues, orieSamples, emgDraw,
								emgRmsDraw, orieDraw, myoLock, jfgsEmg, jfgsOrie, width, rmsSampleNumber,
								neuralNetworkModel[0], nnOutput, ev3thread[0]);
						hub.addListener(myoDataCollector[0]);

						// handle buttons
						buttonLiveStream.setEnabled(false);
						buttonLoadEMG.setEnabled(false);
						buttonSave.setEnabled(true);
						buttonStopAndReset.setEnabled(true);
					}
				});

				// SAVE EMG AND ORIENTATION TO FILE TEXTBOX AND BUTTON LOGIC
				final JTextField textFilenameSave = new JTextField("test", 10);
				buttonSave.setEnabled(false);
				buttonSave.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						myoLock.lock();

						// clip the size or ArrayLists to match minimal size
						emgSamples.set(0,
								new ArrayList<byte[]>(emgSamples.get(0).subList(0, orieSamples.get(0).size())));
						rmsValues.set(0, new ArrayList<int[]>(rmsValues.get(0).subList(0, orieSamples.get(0).size())));

						String strEMG = "";
						String strRMS = "";
						String ending = "\n";
						// format is:
						// first readings (all channels),
						// second readings (all channels),
						// etc. + (newlines between all values)
						for (int i = 0; i < emgSamples.get(0).size(); i++) {
							for (int ii = 0; ii < emgSamples.get(0).get(i).length; ii++) {
								if (i == emgSamples.get(0).size() - 1 && ii == emgSamples.get(0).get(i).length - 1) {
									ending = "";
								}
								strEMG += Byte.toString(emgSamples.get(0).get(i)[ii]) + ending;
								strRMS += Integer.toString(rmsValues.get(0).get(i)[ii]) + ending;
							}
						}
						String strOrie = "";
						ending = "\n";
						for (int i = 0; i < orieSamples.get(0).size(); i++) {
							for (int ii = 0; ii < orieSamples.get(0).get(i).length; ii++) {
								if (i == orieSamples.get(0).size() - 1 && ii == orieSamples.get(0).get(i).length - 1) {
									ending = "";
								}
								strOrie += Double.toString(orieSamples.get(0).get(i)[ii]) + ending;
							}
						}

						myoLock.unlock();

						try {
							BufferedWriter writer = new BufferedWriter(
									new FileWriter("../_readings/EMG" + textFilenameSave.getText() + ".txt"));
							writer.write(strEMG);
							writer.close();

							writer = new BufferedWriter(
									new FileWriter("../_readings/RMS" + textFilenameSave.getText() + ".txt"));
							writer.write(strRMS);
							writer.close();

							writer = new BufferedWriter(
									new FileWriter("../_readings/IMU" + textFilenameSave.getText() + ".txt"));
							writer.write(strOrie);
							writer.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				});

				// LOAD EMG AND ORIENTATION FROM FILE TEXTBOX AND BUTTON LOGIC
				final JTextField textFilenameLoad = new JTextField("test", 10);
				buttonLoadEMG.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						// FAKE MYO LOGIC
						try {
							ArrayList<String> linesEmg = (ArrayList<String>) Files.readAllLines(Paths
									.get("../_readings/EMG" + textFilenameLoad.getText() + ".txt").toAbsolutePath(),
									Charset.defaultCharset());
							ArrayList<String> linesImu = (ArrayList<String>) Files.readAllLines(Paths
									.get("../_readings/IMU" + textFilenameLoad.getText() + ".txt").toAbsolutePath(),
									Charset.defaultCharset());
							final int readingsPerChannel = linesEmg.size() / 8;
							final ArrayList<byte[]> fakiesEmg = new ArrayList<byte[]>();
							final ArrayList<double[]> fakiesImu = new ArrayList<double[]>();
							int counterEmg = 0;
							int counterImu = 0;
							for (int i = 0; i < readingsPerChannel; i++) {
								// EMG
								byte[] bytes = new byte[8];
								for (int ii = 0; ii < 8; ii++) {
									bytes[ii] = Byte.valueOf(linesEmg.get(counterEmg));
									counterEmg++;
								}
								fakiesEmg.add(bytes);

								// IMU
								double[] doubles = new double[3];
								for (int ii = 0; ii < 3; ii++) {
									doubles[ii] = Double.valueOf(linesImu.get(counterImu));
									counterImu++;
								}
								fakiesImu.add(doubles);
							}
							final MyoDataCollector dataCollector = new MyoDataCollector(emgSamples, rmsValues,
									orieSamples, emgDraw, emgRmsDraw, orieDraw, myoLock, jfgsEmg, jfgsOrie, width,
									rmsSampleNumber, neuralNetworkModel[0], nnOutput, ev3thread[0]);
							final int[] firedCount = new int[1];
							fakeTask[0] = fakeScheduler.scheduleAtFixedRate(new Runnable() {
								public void run() {
									dataCollector.onEmgData(null, 0, fakiesEmg.get(firedCount[0]));
									dataCollector.onImu(fakiesImu.get(firedCount[0]));
									firedCount[0]++;
									if (firedCount[0] == readingsPerChannel) {
										// clear the buffer
										myoLock.lock();

										emgSamples.get(0).clear();
										rmsValues.get(0).clear();
										orieSamples.get(0).clear();

										myoLock.unlock();

										// handle buttons
										buttonLiveStream.setEnabled(liveStreamEnabled[0]);
										buttonLoadEMG.setEnabled(true);
										buttonStopAndReset.setEnabled(false);

										// stop calling
										fakeTask[0].cancel(false);
									}
								}
							}, 0, 5, TimeUnit.MILLISECONDS);

							// handle buttons
							buttonLiveStream.setEnabled(false);
							buttonLoadEMG.setEnabled(false);
							buttonStopAndReset.setEnabled(true);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				});

				// COMPONENTS TO PANEL AND PANEL TO FRAME
				JPanel panel = new JPanel();
				panel.add(buttonLiveStream);
				panel.add(textFilenameSave);
				panel.add(buttonSave);
				panel.add(textFilenameLoad);
				panel.add(buttonLoadEMG);
				panel.add(buttonStopAndReset);
				frame.getContentPane().add(panel);

				JPanel secondPanel = new JPanel();
				secondPanel.add(nnOutput);
				frame.getContentPane().add(secondPanel);

				frame.setVisible(true);
			}
		}
	}
}
