package tools.descartes.autoscaling.dummy.cloudfoundry;

import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.CloudFoundryProperties;
import tools.descartes.autoscaling.dummy.config.MetricsProperties;

/**
 * A wrapper class which instantiates a concrete CFConnector
 */
@Component
public class CFConnectorWrapper {
    private final CFConnector cfConnector;

    public CFConnectorWrapper(CloudFoundryProperties cfProperties, MetricsProperties metricsProperties) {
        cfConnector = new LibCFConnector(cfProperties, metricsProperties.getEurekaToCloudfoundryNames());
    }

    /**
     * Returns the wrapped CFConnector
     * @return the wrapped CFConnector
     */
    public CFConnector getCfConnector() {
        return cfConnector;
    }
}
