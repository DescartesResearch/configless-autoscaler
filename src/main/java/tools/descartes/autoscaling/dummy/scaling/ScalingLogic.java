package tools.descartes.autoscaling.dummy.scaling;

import java.util.List;
import java.util.Map;

import tools.descartes.autoscaling.dummy.metrics.Metric;

/**
 * The most abstract interface for an autoscaler
 */
@FunctionalInterface
public interface ScalingLogic {
    /**
     * @param measurements current measurements
     * @param lastActions last executed actions
     * @return scaling decisions
     */
    List<ScalingRequest> calculateScaling(List<Metric> measurements, Map<String, Integer> lastActions);
}
