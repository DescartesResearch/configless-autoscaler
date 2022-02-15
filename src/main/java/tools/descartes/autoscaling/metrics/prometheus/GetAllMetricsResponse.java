package tools.descartes.autoscaling.metrics.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A datatype class for deserializing a call to the Prometheus HTTP API /api/v1/label/__name__/values
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAllMetricsResponse {

    /*
    Example:
    {
    "status": "success",
    "data": [
    "mymetric"
    ]
    }
     */

    @JsonProperty("status")
    public String status;

    @JsonProperty("data")
    public List<String> data;

}
