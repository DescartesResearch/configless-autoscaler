package tools.descartes.autoscaling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A property class containing settings about processing imported metrics
 */
@Component
@ConfigurationProperties("metrics")
@Data
public class MetricsProperties {
    /**
     * A map which has Eureka app names as keys to CloudFoundry app names as values
     */
    private Map<String, String> eurekaToCloudfoundryNames = new HashMap<>();

    /**
     * All metric names which should be considered for the scaling decision should match this regex.
     * We use Matcher.find not Matcher.matches for matching.
     * If it is not set, all metrics will be considered that have an app label.
     */
    private String includeregex;

    /**
     * Returns an Optional containing the Eureka app name for the provided CloudFoundry app name
     * @param cloudfoundryName app name in CloudFoundry
     * @return an Optional containing the Eureka app name for the provided CloudFoundry app name
     */
    public Optional<String> getEurekaNameForCloudFoundryName(String cloudfoundryName) {
        return eurekaToCloudfoundryNames.entrySet().stream()
                .filter(e -> e.getValue().equals(cloudfoundryName))
                .map(Map.Entry::getKey)
                .findAny();
    }
}
