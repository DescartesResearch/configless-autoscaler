package tools.descartes.autoscaling.metrics.prometheus;

import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.config.CloudFoundryProperties;

/**
 * Wrapps and instantiates a concrete implementation of MetricAggregator
 */
@Component
public class MetricAggregatorWrapper {
    private final MetricAggregator aggregator;

    public MetricAggregatorWrapper(CloudFoundryProperties cfProperties) {
        aggregator = new SimpleMetricAggregator(cfProperties.getIncludeappregex(), cfProperties.getExcludeappregex());
    }

    /**
     * @return the wrapped MetricAggregator
     */
    public MetricAggregator getAggregator() {
        return aggregator;
    }
}
