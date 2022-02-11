package tools.descartes.autoscaling.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A property class containing settings for model training
 */
@Data
@Component
@ConfigurationProperties("training")
public class TrainingProperties {
    /**
     * Settings for the export of training data
     */
    private ExportProperties exporter = new ExportProperties();

    /**
     * Settings for the import of training data
     */
    private ImportProperties importer = new ImportProperties();

    /**
     * Retraining interval in seconds
     */
    private int interval;

    /**
     * How many days should be used for training
     */
    private int trainingLookbackDays = 32;

    /**
     * How many days of training data should be kept in the database. Older data is deleted periodically.
     */
    private int trainingDatabaseNumberDaysToKeep = 92;

    @Data
    public static class ExportProperties {
        private boolean enabled = false;

        /**
         * A regex for metrics which should be excluded for exporting
         * We use Matcher.find() not Matcher.matches() for matching
         */
        private String metricStorageExclusionRegex;
        private boolean storeMetricsInDatabase = false;
    }

    @Data
    public static class ImportProperties {
        boolean importMetricsFromDatabase = false;

        /**
         * Paths where to look for CSV files for training
         */
        private List<String> sources;
    }
}
