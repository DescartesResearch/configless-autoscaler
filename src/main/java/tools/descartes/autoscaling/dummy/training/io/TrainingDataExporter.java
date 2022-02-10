package tools.descartes.autoscaling.dummy.training.io;

import tools.descartes.autoscaling.dummy.metrics.Metric;

import java.util.List;
import java.util.Map;

public interface TrainingDataExporter {
    /**
     * Export actions at a specified time and the measurements that had been used for the decision
     * @param time current time
     * @param measurements measurements that had been used to calculate the actions
     * @param actions actions which have been executed
     */
    void commitActions(long time, List<Metric> measurements, Map<String, Integer> actions);

    /**
     * Export measurements at a specified time
     * @param time current time
     * @param measurements measurements
     */
    default void commitMeasurements(long time, List<Metric> measurements) { }

}
