package tools.descartes.autoscaling.metrics;

import java.util.List;

/**
 * Implementing classes provide a way to obtain measurement data
 */
public interface RuntimeDataImporter {
    /**
     * @return the list containing all metric names
     */
    List<String> getAllMetricNames();

    /**
     * @param metricName name of the metric to be queried
     * @param windowInSeconds temporal difference to use for averaging
     * @return the averaged value of this metric over the defined horizon
     */
    List<Metric> getAveragedValuesForMetric(String metricName, int windowInSeconds);

    /**
     * Queries the latest value of single metric
     * @param metricName name or query key of this metric
     * @return an instance of RewritableMetric
     */
    RewritableMetric getSingleMetric(String metricName);
}
