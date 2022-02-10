package tools.descartes.autoscaling.dummy.training.io.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.descartes.autoscaling.dummy.config.TrainingProperties;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataExporter;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An implementation of TrainingDataExporter which exports training data to a database using JDBC
 */
@Component
public class JdbcTrainingDataExporter implements TrainingDataExporter {

    private static final Logger logger = LoggerFactory.getLogger(JdbcTrainingDataExporter.class);

    private final ObjectMapper objectMapper;
    private final MeasurementJdbcRepository measurementJdbcRepository;
    private Pattern metricStorageExclusionPattern = null;

    public JdbcTrainingDataExporter(
            ObjectMapper objectMapper, MeasurementJdbcRepository measurementJdbcRepository,
            TrainingProperties trainingProperties
    ) {
        this.objectMapper = objectMapper;
        this.measurementJdbcRepository = measurementJdbcRepository;
        if (trainingProperties.getExporter() != null
                && StringUtils.hasText(trainingProperties.getExporter().getMetricStorageExclusionRegex())) {
            metricStorageExclusionPattern = Pattern.compile(
                    trainingProperties.getExporter().getMetricStorageExclusionRegex());
        }
    }


    @Override
    public void commitActions(long time, List<Metric> measurements, Map<String, Integer> actions) {
        Set<String> apps = measurements.stream().map(Metric::getApp).collect(Collectors.toSet());
        logger.debug("Committing actions for apps={}", apps);
        Map<String, Integer> actionsMap = new HashMap<>(actions);
        apps.forEach(app -> {
            if (!actionsMap.containsKey(app)) actionsMap.put(app, 0);
        });
        try {
            List<MeasurementJdbcEntity> entities = new ArrayList<>();
            LocalDateTime measurementTime = systemMillisToLocalDateTime(time);
            for (String app : measurements.stream().map(Metric::getApp).collect(Collectors.toSet())) {
                List<Metric> measurementsOfApp = measurements.stream()
                        .filter(metric -> metric.getApp().equals(app))
                        .filter(metric -> metricStorageExclusionPattern == null
                                || !metricStorageExclusionPattern.matcher(metric.getName()).find())
                        .map(metric -> new JsonMetric(metric.getName(), metric.getValue()))
                        .collect(Collectors.toList());
                logger.trace("Storing metrics for app={}: {}", app, measurementsOfApp);
                try {
                    entities.add(new MeasurementJdbcEntity(measurementTime, app,
                            objectMapper.writeValueAsString(measurementsOfApp), actionsMap.get(app)));
                } catch (JsonProcessingException e) {
                    logger.error("Error writing metrics to byte array: ", e);
                    throw new UncheckedIOException(e);
                }
            }
            measurementJdbcRepository.insert(entities);
        } catch (UncheckedIOException e) {
            logger.error("Caught exception committing data to db, skipping ....", e);
        }
    }

    public static LocalDateTime systemMillisToLocalDateTime(long systemMillisTimeStamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(systemMillisTimeStamp), ZoneId.systemDefault());
    }

    public static long localDateTimeToSystemTimeMillis(LocalDateTime timeStamp) {
        return timeStamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
