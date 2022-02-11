package tools.descartes.autoscaling.scaling;

/**
 * A request class containing information about which app to scale to which number of instances
 */
public class ScalingRequest {

    private final String app;
    private final int instances;

    public ScalingRequest(String app, int instances) {
        this.app = app;
        this.instances = instances;
    }

    public int getInstances() {
        return instances;
    }

    public String getApp() {
        return app;
    }

    @Override
    public String toString() {
        return "Scale " + app + " to " + instances + " instances";
    }
}
