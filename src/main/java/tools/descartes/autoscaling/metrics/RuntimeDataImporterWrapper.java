package tools.descartes.autoscaling.metrics;

import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.config.PrometheusProperties;
import tools.descartes.autoscaling.metrics.prometheus.MetricAggregatorWrapper;
import tools.descartes.autoscaling.metrics.prometheus.PrometheusConnector;

/**
 * A wrapper class which instantiates a concrete implementation of RuntimeDataImporter
 */
@Component
public class RuntimeDataImporterWrapper {
    private final RuntimeDataImporter runtimeDataImporter;

    public RuntimeDataImporterWrapper(PrometheusProperties prometheusProperties, MetricAggregatorWrapper metricAggregatorWrapper) {
        runtimeDataImporter = new PrometheusConnector(prometheusProperties.getUrl(), metricAggregatorWrapper.getAggregator());
    }

    public RuntimeDataImporter getRuntimeDataImporter() {
        return runtimeDataImporter;
    }
}
