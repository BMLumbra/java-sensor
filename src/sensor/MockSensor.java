package sensor;

public class MockSensor implements Sensor {
	class MockPollingThread extends PollingThread {
		@Override
		public void poll() {
			synchronized (queuedDataLock) {
				queuedData = Math.random();
			}
		}
	}
	PollingThread pollingThread = new MockPollingThread();
	
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
		pollingThread.start();
	}

	@Override
	public void setPollingRate(double rateHz) {
		pollingThread.setPollingRate(rateHz);
	}

	@Override
	public String getUnitsString() {
		return "Units";
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
				long sleepTime = (long)(1000 / pollingThread.getPollingRate());
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

	@Override
	public double getData() {
		return pollingThread.getData();
	}

}
