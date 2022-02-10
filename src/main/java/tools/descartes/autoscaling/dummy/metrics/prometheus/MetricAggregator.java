package tools.descartes.autoscaling.dummy.metrics.prometheus;

import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.RewritableMetric;

import java.util.List;

/**
 * Implementing classes provide ways the process Matrix- or Vector results from Prometheus
 */
public interface MetricAggregator {
    /**
     * Processes a VectorResult and returns an RewritableMetric
     * @param vector
     * @return
     */
    RewritableMetric aggregateMetrics(VectorResult vector);

    /**
     * Processes a MatrixResult and returns a list of metrics, which could multiple metrics for multiple apps
     * @param matrix
     * @return
     */
    List<Metric> aggregateMetrics(MatrixResult matrix);
}
