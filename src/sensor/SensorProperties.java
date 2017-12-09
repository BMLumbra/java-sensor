package sensor;

public class SensorProperties {
	private double fullScaleValue;
	private final int bitsResolution;
	private final String unitsStr;
	
	public SensorProperties(double maxValue, int resolution, String units) {
		fullScaleValue = maxValue;
		bitsResolution = resolution;
		unitsStr = units;
	}
	
	public double getFullScaleValue() {
		return fullScaleValue;
	}
	
	public int getResolutionBits() {
		return bitsResolution;
	}
	
	public int getResolutionBytes() {
		return bitsResolution / 8;
	}
	
	public String getUnit() {
		return unitsStr;
	}
	
	public void setFullScaleValue(double newFullScale) {
		fullScaleValue = newFullScale;
	}
}
