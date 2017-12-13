package sensor;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
		private InputStream inStream;
		private OutputStream outStream;
		
		@Override
		public void poll() {
			byte inBuf[] = new byte[8];
			byte outBuf[] = new byte[1];
			int numBytes = 0;

			while (true) {
				try {
					outBuf[0] = '\0';
					outStream.write(outBuf);
				} catch (IOException e) {
					Log.d(logTag, "Input stream disconnected", e);
					break;					
				}
				try {
					numBytes = inStream.read(inBuf);
					if (numBytes > properties.getResolutionBytes()) {
						Log.e(logTag, "Sensor sent too many bytes!");
					}
					ByteBuffer bufferStream = ByteBuffer.wrap(inBuf);
					bufferStream.order(ByteOrder.BIG_ENDIAN);
					synchronized (queuedDataLock) {
						queuedData = bufferStream.getDouble() * properties.getFullScaleValue();
					}
				} catch (IOException e) {
					Log.d(logTag, "Input stream disconnected", e);
					break;
				}
			}	
		}
		
		@Override
		public void run() {
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
			
			super.run();
		}
		
		/*private void write(byte[] bytes) {
			try {
				outStream.write(bytes);
			} catch (IOException e) {
				Log.e(logTag, "Error sending data on OutputStream!", e);
			}
		}*/
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
	
	public AndroidBTSensor(BluetoothAdapter adapter, BluetoothDevice sensorDevice, String appTag, SensorProperties props) {
		this(adapter, sensorDevice, appTag, props, 1.0d);
	}
	
	public AndroidBTSensor(BluetoothAdapter adapter, BluetoothDevice sensorDevice, String appTag, SensorProperties props, double rateHz) {
		btAdapter = adapter;
		this.sensorDevice = sensorDevice;
		uuid = sensorDevice.getUuids()[0].getUuid();
		logTag = appTag;
		pollingThread = new BTPollingThread();
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
	}
	
	@Override
	public void startPolling() {
		pollingThread.start();
	}

	@Override
	public void setPollingRate(double rateHz) {
		pollingThread.setPollingRate(rateHz);	
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
			public void run() {
				try {
					Thread.sleep(getPollingPeriodMs());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		
		try {
			waitingThread.join();
		} catch (InterruptedException e) {
			Log.e(logTag, "Waiting thread interrupted!", e);
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
