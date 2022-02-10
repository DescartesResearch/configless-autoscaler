package tools.descartes.autoscaling.dummy.metrics;

/**
 * An extension of Metric which allows overwriting metric name and app (needed for KPI queries)
 */
public interface RewritableMetric extends Metric {
    void setName(String name);
    void setApp(String app);
}
