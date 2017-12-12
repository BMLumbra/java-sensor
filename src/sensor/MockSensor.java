package sensor;

public class MockSensor implements Sensor {
	double queuedDataValue = 0.0d;
	double pollingRateHz = 1.0d;
	Object queuedDataLock = new Object();
	Object pollingRateLock = new Object();
	
	public MockSensor() {
		
	}

	@Override
	public void connect() {
		return;
	}

	@Override
	public void disconnect() {
		return;
	}

	@Override
	public void startPolling() {
		Thread pollThread = new Thread(new Runnable() {
			@Override
			public void run() {
				long sleepTime = 0;
				while (true) {
					synchronized (queuedDataLock) {
						queuedDataValue = Math.random();
					}
					try {
						synchronized (pollingRateLock) {
							sleepTime = (long)(1000 / pollingRateHz);
						}
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		pollThread.start();
	}

	@Override
	public void setPollingRate(double rateHz) {
		synchronized (pollingRateLock) {
			pollingRateHz = rateHz;
		}
	}

	@Override
	public String getUnitsString() {
		return "Units";
	}

	@Override
	public double getPollingRate() {
		return pollingRateHz;
	}

	@Override
	public void waitForNextDataPoint() {
		Thread waitingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				long sleepTime = 0;
				try {
					synchronized (pollingRateLock) {
						sleepTime = (long)(1000 / pollingRateHz);
					}
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		
		waitingThread.start();
		try {
			waitingThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public double getData() {
		synchronized (queuedDataLock) {
			return queuedDataValue;
		}
	}

}
