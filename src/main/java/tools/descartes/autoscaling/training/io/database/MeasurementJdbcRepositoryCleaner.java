package tools.descartes.autoscaling.training.io.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.config.TrainingProperties;

import java.time.LocalDateTime;

@EnableScheduling
@Component
public class MeasurementJdbcRepositoryCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementJdbcRepositoryCleaner.class);

    private final MeasurementJdbcRepository measurementJdbcRepository;
    private final long daysToKeep;

    public MeasurementJdbcRepositoryCleaner(MeasurementJdbcRepository measurementJdbcRepository, TrainingProperties trainingProperties) {
        this.measurementJdbcRepository = measurementJdbcRepository;
        this.daysToKeep = trainingProperties.getTrainingDatabaseNumberDaysToKeep();
    }

    @Scheduled(cron = "0 15 1 * * SUN")
    public void removeOutdatedMeasurements() {
        LocalDateTime removeOlderThan = LocalDateTime.now().minusDays(daysToKeep);
        logger.info("Removing outdated measurements older than {}", removeOlderThan);
        measurementJdbcRepository.deleteAllMeasurementsBefore(removeOlderThan);
    }
}
