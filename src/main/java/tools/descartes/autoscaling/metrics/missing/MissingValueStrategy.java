package tools.descartes.autoscaling.metrics.missing;

/**
 * Implementing classes define which value to use in cases required metrics cannot be measured
 */
@FunctionalInterface
public interface MissingValueStrategy {

    /**
     * @return a value which should be used when the measurement of a required metric is not available
     */
    double getDefaultValue();
}
