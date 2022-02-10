package tools.descartes.autoscaling.dummy.metrics.prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * A datatype class for deserializing a MatrixResult from Prometheus
 */
public class MatrixResult {

    /*
    Example:
    {
    "status": "success",
    "data": <SOME DATA FORMAT>
    }
     */

    @JsonProperty("status")
    public String status;

    @JsonProperty("data")
    public MatrixResultData data;

    public static class MatrixResultData {

    /*
    Example:
    {
    "resultType": "matrix",
    "result": [
    {
    "metric": {
    "__name__": "CPU",
    "app": "loon-service",
    "someOtherLabel": "someOtherValue"
    },
    "values": [
    [ 14354545334.82, "0.43552234"]
    ]
    }
    ]
    }
     */

        @JsonProperty("resultType")
        public String resultType;

        @JsonProperty("result")
        public List<MatrixEntry> result;

        public static class MatrixEntry {

            @JsonProperty("metric")
            public Map<String, String> metric;

            @JsonProperty("values")
            public List<List<Object>> values;

            @Override
            public String toString() {
                return "MatrixEntry{" +
                        "metric=" + metric +
                        ", values=" + values +
                        '}';
            }
        }
    }
}
