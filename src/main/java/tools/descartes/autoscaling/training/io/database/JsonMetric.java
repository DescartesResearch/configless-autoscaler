package tools.descartes.autoscaling.training.io.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.descartes.autoscaling.metrics.SimpleMetric;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonMetric extends SimpleMetric {
    @JsonCreator
    public JsonMetric(@JsonProperty("name") String name, @JsonProperty("value") double value) {
        super(name, null, value);
    }
}
