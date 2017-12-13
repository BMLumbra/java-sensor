package sensor;

public abstract class PollingThread extends Thread {
	double pollingRateHz = 0.0d;
	double queuedData = 0.0d;
	Object pollingRateLock = new Object();
	Object queuedDataLock = new Object();
	
	public PollingThread() {
		pollingRateHz = 1.0d;
	}
	
	public PollingThread(double pollingRate) {
		pollingRateHz = pollingRate;
	}
	
	public void poll() {
		return;
	}
	
	@Override
	public void run() {
		long sleepTime = 0;
		while (true) {
			poll();
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
	
	public void setPollingRate(double pollingRate) {
		synchronized (pollingRateLock) {
			pollingRateHz = pollingRate;
		}		
	}
	
	public double getPollingRate() {
		synchronized (pollingRateLock) {
			return pollingRateHz;
		}
	}
	
	public double getData() {
		synchronized (queuedDataLock) {
			return queuedData;
		}
	}
}
