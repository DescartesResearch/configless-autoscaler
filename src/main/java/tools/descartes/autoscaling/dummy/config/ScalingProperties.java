package tools.descartes.autoscaling.dummy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * A property class containing settings for the autoscaler
 */
@Component
@ConfigurationProperties("scaling")
public class ScalingProperties {
    /**
     * Defines whether the autoscaler should start automatically at startup or starts in an idle state
     */
    @Getter
    @Setter
    private boolean autostart = false;

    /**
     * Temporal difference between two scaling decisions
     */
    private int timeout;

    /**
     * The metric which contains the number of instances for the apps
     */
    private String instances;

    /**
     * Timeout (temporal difference between two upscaling decisions) and limit (maximum number of instances which can
     * be added) for upscaling
     */
    private ScalingDirectionProperties upscale;

    /**
     * Timeout (temporal difference between two downscaling decisions) and limit (maximum number of instances which can
     * be removed) for downscaling
     */
    private ScalingDirectionProperties downscale;

    /**
     * Settings for the Safety component
     */
    private SafetyProperties safety;

    /**
     * A map which contains (Eureka) app names as keys and KPIs as values
     * Each KPI contains a metric name and an upper bound (SLO)
     */
    private Map<String, List<KPIProperties>> kpis;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getInstances() {
        return instances;
    }

    public void setInstances(String instances) {
        this.instances = instances;
    }

    public ScalingDirectionProperties getUpscale() {
        return upscale;
    }

    public void setUpscale(ScalingDirectionProperties upscale) {
        this.upscale = upscale;
    }

    public ScalingDirectionProperties getDownscale() {
        return downscale;
    }

    public void setDownscale(ScalingDirectionProperties downscale) {
        this.downscale = downscale;
    }

    public SafetyProperties getSafety() {
        return safety;
    }

    public void setSafety(SafetyProperties safety) {
        this.safety = safety;
    }

    public Map<String, List<KPIProperties>> getKpis() {
        return kpis;
    }

    public void setKpis(Map<String, List<KPIProperties>> kpis) {
        this.kpis = kpis;
    }

    public String showEditableEntries(String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append("scaling.upscale.limit = ");
        builder.append(this.getUpscale().getLimit());
        builder.append(separator);
        builder.append("scaling.upscale.timeout = ");
        builder.append(this.getUpscale().getTimeout());
        builder.append(separator);
        builder.append("scaling.downscale.limit = ");
        builder.append(this.getDownscale().getLimit());
        builder.append(separator);
        builder.append("scaling.downscale.timeout = ");
        builder.append(this.getDownscale().getTimeout());
        builder.append(separator);
        return builder.toString();
    }

    public String showKPIs(String separator) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<KPIProperties>> entry : getKpis().entrySet()) {
            builder.append(entry.getKey());
            builder.append(":");
            builder.append(separator);
            builder.append("    ");
            for (KPIProperties kpisForApp : entry.getValue()) {
                builder.append(kpisForApp.getName());
                builder.append(" ");
                builder.append(kpisForApp.getSlo());
                builder.append(separator);
                builder.append("    ");
            }
            builder.delete(builder.length() - 4, builder.length());
        }
        return builder.toString();
    }

    public static class ScalingDirectionProperties {
        private int timeout;
        private int limit;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
    }

    public static class SafetyProperties {
        private int period;
        private double ratio;

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        public double getRatio() {
            return ratio;
        }

        public void setRatio(double ratio) {
            this.ratio = ratio;
        }
    }

    public static class KPIProperties {
        private String name;
        private double slo;
        private String query;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getSlo() {
            return slo;
        }

        public void setSlo(double slo) {
            this.slo = slo;
        }

        public String getQuery() {
            if (query == null) return name;
            else return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }
}
