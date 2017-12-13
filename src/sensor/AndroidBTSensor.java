package sensor;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class AndroidBTSensor implements BTSensor {
	public class ConnectionThread extends Thread {		
		public ConnectionThread() {
			try {
				sensorSocket = sensorDevice.createRfcommSocketToServiceRecord(uuid);
			} catch (IOException e) {
				Log.e(logTag, "Unable to connect to Bluetooth device!", e);
			}
		}
		
		public void run() {
			btAdapter.cancelDiscovery();
			
			try {
				sensorSocket.connect();
			} catch (IOException connectException) {
				try {
					sensorSocket.close();
				} catch (IOException closeException) {
					Log.e(logTag, "Unable to close client socket connection!", closeException);
				}
			}
		}
	}
	
	public class BTPollingThread extends PollingThread {
		private final InputStream inStream;
		private final OutputStream outStream;
		
		public BTPollingThread() {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try {
				tmpIn = sensorSocket.getInputStream();
			} catch (IOException e) {
				Log.e(logTag, "Unable to get input stream from socket!", e);
			}
			try {
				tmpOut = sensorSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(logTag, "Unable to get output stream from socket!", e);
			}
			
			inStream = tmpIn;
			outStream = tmpOut;
		}
		
		@Override
		public void poll() {
			try {
				DataInputStream dataStream = new DataInputStream(inStream);
				synchronized (queuedDataLock) {
					queuedData = (double)dataStream.readFloat() * properties.getFullScaleValue();
				}
			} catch (IOException e) {
				Log.e(logTag, "Input stream disconnected, exiting", e);
				return;
			}
		}
		
		@Override
		public void setPollingRate(double rateHz) {
			byte payload[] = new byte[2];
			payload[0] = 1;
			payload[1] = (byte)rateHz;
			write(payload);
			
			super.setPollingRate(rateHz);
		}
		
		private void write(byte[] bytes) {
			try {
				outStream.write(bytes);
			} catch (IOException e) {
				Log.e(logTag, "Error sending data on OutputStream!", e);
			}
		}
	}
	
	public class DisconnectionThread extends Thread {
		public void run() {
			try {
				sensorSocket.close();
			} catch (IOException e) {
				Log.e(logTag, "Unable to close client socket connection!", e);
			}
		}
	}
	
	private BluetoothDevice sensorDevice = null;
	private BluetoothSocket sensorSocket = null;
	private BluetoothAdapter btAdapter = null;
	private UUID uuid = null;
	private final String logTag;
	private SensorProperties properties = null;
	private PollingThread pollingThread = null;
	boolean connected = false;
	double initPollingRate = 0.0d;
	
	public AndroidBTSensor(BluetoothAdapter adapter, BluetoothDevice sensorDevice, String appTag, SensorProperties props) {
		this(adapter, sensorDevice, appTag, props, 1.0d);
	}
	
	public AndroidBTSensor(BluetoothAdapter adapter, BluetoothDevice sensorDevice, String appTag, SensorProperties props, double rateHz) {
		btAdapter = adapter;
		this.sensorDevice = sensorDevice;
		uuid = sensorDevice.getUuids()[0].getUuid();
		logTag = appTag;
		setPollingRate(rateHz);
		properties = props;
	}

	@Override
	public void connect() {
		Thread connectionThread = new ConnectionThread();
	    connectionThread.start();
	    try {
	    	connectionThread.join();
	    } catch (InterruptedException e) {
	    	Log.e(logTag, "Connection thread interrupted!", e);
	    }
	    connected = true;
		pollingThread = new BTPollingThread();
	}

	@Override
	public void disconnect() {
		Thread disconnectionThread = new DisconnectionThread();
	    disconnectionThread.start();
	    try {
	    	disconnectionThread.join();
	    } catch (InterruptedException e) {
	    	Log.e(logTag, "Connection thread interrupted!", e);
	    }
	    connected = false;
	}
	
	@Override
	public void startPolling() {
		pollingThread.start();
	    setPollingRate(initPollingRate);
	}

	@Override
	public void setPollingRate(double rateHz) {
		if (connected && pollingThread.isAlive()) {
			pollingThread.setPollingRate(rateHz);
		} else {
			initPollingRate = rateHz;
		}
	}

	@Override
	public String getUnitsString() {
		return properties.getUnit();
	}

	@Override
	public double getPollingRate() {
		return pollingThread.getPollingRate();
	}

	@Override
	public void waitForNextDataPoint() {
		Thread waitingThread = new Thread() {
			@Override
			public void run() {
				long sleepTime = getPollingPeriodMs();
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}};
		
		waitingThread.start();
		try {
			waitingThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private long getPollingPeriodMs() {
		return (long)(1000 / getPollingRate());
	}

	@Override
	public double getData() {
		return pollingThread.getData();
	}
	
}
