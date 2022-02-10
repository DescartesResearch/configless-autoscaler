package tools.descartes.autoscaling.dummy.metrics.missing;

/**
 * A MissingValueStrategy which simply puts a constant for missing values
 */
public class ConstantStrategy implements MissingValueStrategy {

    private final double defaultValue;

    public ConstantStrategy(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public double getDefaultValue() {
        return defaultValue;
    }
}
