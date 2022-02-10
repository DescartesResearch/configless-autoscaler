package tools.descartes.autoscaling.dummy.training.io.database;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@EnableScheduling
@Component
@RequiredArgsConstructor
public class MeasurementJdbcRepositoryCleaner {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementJdbcRepositoryCleaner.class);

    private static final long DAYS_TO_KEEP = 93;

    private final MeasurementJdbcRepository measurementJdbcRepository;

    @Scheduled(cron = "0 15 1 * * SUN")
    public void removeOutdatedMeasurements() {
        LocalDateTime removeOlderThan = LocalDateTime.now().minusDays(DAYS_TO_KEEP);
        logger.info("Removing outdated measurements older than {}", removeOlderThan);
        measurementJdbcRepository.deleteAllMeasurementsBefore(removeOlderThan);
    }
}
