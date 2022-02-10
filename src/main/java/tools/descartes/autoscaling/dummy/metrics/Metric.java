package tools.descartes.autoscaling.dummy.metrics;

/**
 * A concrete measurement defined as a triple of a metric name, a value and an app
 */
public interface Metric {
    /**
     * @return name of this metric
     */
    String getName();

    /**
     * @return value of this metric
     */
    double getValue();

    /**
     * @return app where this metric belongs to
     */
    String getApp();

    static Metric of(String name, double value, String app) {
        return new SimpleMetric(name, app, value);
    }
}
