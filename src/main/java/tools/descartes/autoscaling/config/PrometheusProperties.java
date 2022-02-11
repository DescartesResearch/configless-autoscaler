package tools.descartes.autoscaling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A property class containing settings about the Prometheus server
 */
@Component
@ConfigurationProperties("prometheus")
public class PrometheusProperties {
    private String url;
    private int scrapeDuration;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getScrapeDuration() {
        return scrapeDuration;
    }

    public void setScrapeDuration(int scrapeDuration) {
        this.scrapeDuration = scrapeDuration;
    }
}
