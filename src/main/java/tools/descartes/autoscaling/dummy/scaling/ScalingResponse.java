package tools.descartes.autoscaling.dummy.scaling;

/**
 * Response to a ScalingRequest, if not successful, it contains an error message
 */
public class ScalingResponse {
    private final boolean success;
    private final String message;

    public ScalingResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

}
