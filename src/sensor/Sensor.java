package sensor;

public interface Sensor {
	// Create connection to/from a sensor
	abstract void connect();
	abstract void disconnect();
	abstract void startPolling();
	
	// Adjust sensor parameters
	abstract void setPollingRate(double rateHz);
	
	// Retrieve sensor parameters
	abstract String getUnitsString();
	abstract double getPollingRate();
	
	// Get data from sensor
	abstract void waitForNextDataPoint();
	abstract double getData();
}
