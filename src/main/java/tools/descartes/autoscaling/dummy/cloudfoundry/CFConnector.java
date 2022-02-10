package tools.descartes.autoscaling.dummy.cloudfoundry;

import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;
import tools.descartes.autoscaling.dummy.scaling.ScalingResponse;

import java.util.Map;

/**
 * Implementing classes provide access to a CloudFoundry management plane
 */
public interface CFConnector {

    /**
     * Gets number of (desired) instances for apps on CloudFoundry
     * @return a map containing app names as keys and number of instances as values
     */
    Map<String, Integer> getAppInstances();

    /**
     * Performs scaling defined in the provided ScalingRequest
     * @param sr ScalingRequest containing which app to scale and how many instances
     * @return ScalingResponse describing operation success and possible error message
     */
    ScalingResponse scaleApp(ScalingRequest sr);

}
