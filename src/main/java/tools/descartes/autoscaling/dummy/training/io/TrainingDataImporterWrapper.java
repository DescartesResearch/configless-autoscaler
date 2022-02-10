package tools.descartes.autoscaling.dummy.training.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.TrainingProperties;
import tools.descartes.autoscaling.dummy.training.io.csv.CSVTrainingDataImporter;
import tools.descartes.autoscaling.dummy.training.io.database.JdbcTrainingDataImporter;

/**
 * A wrapper class which instantiates a concrete instance of TrainingDataImporter
 */
@Component
public class TrainingDataImporterWrapper {

    Logger logger = LoggerFactory.getLogger(TrainingDataImporterWrapper.class);

    private final TrainingDataImporter dataImporter;

    public TrainingDataImporterWrapper(
            TrainingProperties trainingProperties,
            JdbcTrainingDataImporter jdbcTrainingDataImporter
    ) {
        if (trainingProperties.getImporter().isImportMetricsFromDatabase()) {
            logger.debug("Importing training data from relational database using jdbc");
            dataImporter = jdbcTrainingDataImporter;
        } else {
            logger.debug("Importing training data from csv files");
            dataImporter = new CSVTrainingDataImporter(trainingProperties.getImporter().getSources());
        }
    }

    public TrainingDataImporter getDataImporter() {
        return dataImporter;
    }
}
