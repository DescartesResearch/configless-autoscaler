package tools.descartes.autoscaling.knowledge;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.cloudfoundry.CFConnectorWrapper;
import tools.descartes.autoscaling.config.MetricsProperties;

/**
 * A wrapper which contains a map for the number of instances for each app and
 * a map for matching CloudFoundry app names and Eureka app names
 */
@Component
public class InstanceManager {

    private static final Logger logger = LoggerFactory.getLogger(InstanceManager.class);

    private final CFConnectorWrapper cfConnector;
    private final MetricsProperties metricsProperties;
    private Map<String, Integer> instanceCounter;

    public InstanceManager(CFConnectorWrapper cfConnector,
                           MetricsProperties metricsProperties) {
        this.cfConnector = cfConnector;
        this.metricsProperties = metricsProperties;
    }

    /**
     * Queries the number of instances from CloudFoundry and reads app name mapping from MetricsProperties
     */
    public void init() {
        instanceCounter = new HashMap<>();
        Map<String, Integer> instances = cfConnector.getCfConnector().getAppInstances();
        for (Map.Entry<String, Integer> instanceInfo: instances.entrySet()) {
            String eurekaName = metricsProperties.getEurekaNameForCloudFoundryName(instanceInfo.getKey())
                    .orElse(instanceInfo.getKey());
            instanceCounter.put(eurekaName, instanceInfo.getValue());
        }
        logger.debug("Got initial instance counts from cloudfoundry: {}", instanceCounter);
    }

    /**
     * Gets number of instances for provided app
     * @param app The app name for the query
     * @return The number of instances for provided app
     */
    public int getCountOf(String app) {
        return instanceCounter.getOrDefault(app, -1);
    }

    /**
     * Sets the number of instances for app to count. Should be called after a successful scaling
     * @param app The app name to update
     * @param count The new number of instances
     * @return difference from new to old value
     */
    public int setCountOf(String app, int count) {
        Integer oldVal = instanceCounter.put(app, count);
        return oldVal != null ? count - oldVal : 0;
    }

    /**
     * Returns the list of known apps.
     *
     * @return A set of a string representation for each app.
     */
    public Set<String> getApps() {
        if (instanceCounter == null) return null;
        else return instanceCounter.keySet();
    }

}
