package tools.descartes.autoscaling.training.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.config.TrainingProperties;
import tools.descartes.autoscaling.metrics.missing.MissingValueStrategyWrapper;
import tools.descartes.autoscaling.training.io.csv.CSVTrainingDataExporter;
import tools.descartes.autoscaling.training.io.database.JdbcTrainingDataExporter;

/**
 * A wrapper class which instantiates a concrete instance of TrainingDataExporter
 */
@Component
public class TrainingDataExporterWrapper {

    Logger logger = LoggerFactory.getLogger(TrainingDataExporterWrapper.class);

    private final TrainingDataExporter exporter;

    public TrainingDataExporterWrapper(
            TrainingProperties trainingProperties,
            MissingValueStrategyWrapper missingValueStrategyWrapper,
            JdbcTrainingDataExporter jdbcTrainingDataExporter
    ) {
        if (trainingProperties.getExporter().isStoreMetricsInDatabase()) {
            logger.debug("Exporting training data to relational database using jdbc");
            exporter = jdbcTrainingDataExporter;
        } else {
            logger.debug("Exporting training data to csv files");
            exporter = new CSVTrainingDataExporter(missingValueStrategyWrapper);
        }
    }

    public TrainingDataExporter getExporter() {
        return exporter;
    }
}
