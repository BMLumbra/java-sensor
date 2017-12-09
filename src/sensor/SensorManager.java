package sensor;

import java.util.HashSet;
import java.util.Set;

public class SensorManager {
	Set<Sensor> sensors = new HashSet<Sensor>();
	
	public SensorManager() {
		return;
	}
	
	public void addSensor(Sensor newSensor) {
		sensors.add(newSensor);
	}
	
	public void removeSensor(Sensor oldSensor) {
		sensors.remove(oldSensor);
	}
}
