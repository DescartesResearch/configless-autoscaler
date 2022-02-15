package tools.descartes.autoscaling.metrics;

/**
 * A simple implementation of the Metric and RewritableMetric interfaces
 */
public class SimpleMetric implements RewritableMetric {

	private String name;
	private String app;
	private final double value;

	public SimpleMetric(String name, String app, double value) {
		this.name = name;
		this.app = app;
		this.value = value;
	}

	@Override
	public double getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getApp() {
		return app;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setApp(String app) {
		this.app = app;
	}

	@Override
	public String toString() {
		return this.name + ": " + this.value;
	}
}
