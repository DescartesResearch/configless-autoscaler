package tools.descartes.autoscaling.dummy.metrics.prometheus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * A datatype class for deserializing VectorResult responses from Prometheus
 */
public class VectorResult {
    /*
    {"status":"success",
    "data":{
        "resultType":"vector",
        "result":[
            {"metric":
                {"uri":"/api/v1/holidays"},
             "value":[
                    1641243600,
                    "2.07873805546275"
             ]
            }
        ]
     }}
     */

    @JsonProperty("status")
    public String status;

    @JsonProperty("data")
    public VectorResultData data;

    public static class VectorResultData {
        @JsonProperty("resultType")
        public String resultType;

        @JsonProperty("result")
        public List<VectorEntry> result;

        public static class VectorEntry {
            @JsonProperty("metric")
            public Map<String, String> metric;

            @JsonProperty("value")
            public List<Object> value;
        }
    }
}
