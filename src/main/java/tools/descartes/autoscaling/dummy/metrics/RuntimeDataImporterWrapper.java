package tools.descartes.autoscaling.dummy.metrics;

import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.PrometheusProperties;
import tools.descartes.autoscaling.dummy.metrics.prometheus.MetricAggregatorWrapper;
import tools.descartes.autoscaling.dummy.metrics.prometheus.PrometheusConnector;

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
