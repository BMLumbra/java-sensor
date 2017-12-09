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
	
	public class PollingThread extends Thread {
		private final InputStream inStream;
		private final OutputStream outStream;
		
		public PollingThread() {
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
		
		public void run() {
			byte buffer[] = new byte[8];
			int numBytes = 0;

			while (true) {
				try {
					numBytes = inStream.read(buffer);
					if (numBytes > properties.getResolutionBytes()) {
						Log.e(logTag, "Sensor sent too many bytes!");
					}
					ByteBuffer bufferStream = ByteBuffer.wrap(buffer);
					bufferStream.order(ByteOrder.BIG_ENDIAN);
					synchronized (queuedValueLock) {
						queuedValue = bufferStream.getDouble() * properties.getFullScaleValue();
					}
				} catch (IOException e) {
					Log.d(logTag, "Input stream disconnected", e);
					break;
				}
				try {
					Thread.sleep(getPollingPeriodMs());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		public void write(byte[] bytes) {
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
	private String logTag = new String();
	private double pollingRateHz = 0.0d;
	private double queuedValue = 0.0d;
	private Object queuedValueLock = new Object();
	private SensorProperties properties = null;
	private PollingThread pollingThread = null;
	
	public AndroidBTSensor(BluetoothDevice sensorDevice, UUID app_uuid, String appTag, float rateHz, SensorProperties props) {
		this.sensorDevice = sensorDevice;
		uuid = app_uuid;
		logTag = appTag;
		pollingRateHz = rateHz;
		properties = props;
	}

	@Override
	public void connect() {
	    (new ConnectionThread()).start();
	}

	@Override
	public void disconnect() {
		(new DisconnectionThread()).start();		
	}
	
	@Override
	public void startPolling() {
		pollingThread = new PollingThread();
		pollingThread.start();
	}

	@Override
	public void setPollingRate(double rateHz) {
		pollingRateHz = rateHz;		
	}

	@Override
	public String getUnitsString() {
		return properties.getUnit();
	}

	@Override
	public double getPollingRate() {
		return pollingRateHz;
	}

	@Override
	public void waitForNextDataPoint() {
		try {
			Thread.sleep(getPollingPeriodMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	private long getPollingPeriodMs() {
		return (long)(1000 / pollingRateHz);
	}

	@Override
	public double getData() {
		synchronized (queuedValueLock) {
			return queuedValue;
		}
	}
	
}
