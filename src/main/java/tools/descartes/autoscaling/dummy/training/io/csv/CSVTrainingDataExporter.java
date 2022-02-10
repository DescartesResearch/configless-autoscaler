package tools.descartes.autoscaling.dummy.training.io.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategy;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategyWrapper;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of TrainingDataExporter which exports data to a CSV file
 */
public class CSVTrainingDataExporter implements TrainingDataExporter {
    /**
     * CSV file format
     * Time,Action,<LIST_OF_METRICS>
     */
    private static final Logger logger = LoggerFactory.getLogger(CSVTrainingDataExporter.class);
    private final Map<String, List<String>> knownMetrics;
    private final Map<String, List<String>> knownMetricsAction;
    private final MissingValueStrategy missingValueStrategy;

    public CSVTrainingDataExporter(MissingValueStrategyWrapper missingValueStrategyWrapper) {
        knownMetrics = new HashMap<>();
        knownMetricsAction = new HashMap<>();
        this.missingValueStrategy = missingValueStrategyWrapper.getDefaultStrategy();
    }

    @Override
    public void commitActions(long time, List<Metric> measurements, Map<String, Integer> actions) {
        Set<String> apps = measurements.stream().map(Metric::getApp).collect(Collectors.toSet());
        for (String app : apps) {
            List<Metric> measurementsOfApp = measurements.stream()
                    .filter(metric -> metric.getApp().equals(app))
                    .collect(Collectors.toList());
            writeToActionFile(app, time, actions.get(app), measurementsOfApp);
        }
    }

    private void writeToActionFile(String app, long time, int action, List<Metric> measurements) {
        if (!knownMetricsAction.containsKey(app)) {
            StringBuilder header = new StringBuilder();
            header.append("time,action");
            knownMetricsAction.put(app, new ArrayList<>());
            measurements.stream().map(Metric::getName).forEach(name -> {
                header.append(",").append(name);
                knownMetricsAction.get(app).add(name);
            });
            File file = new File(app + "_actions.csv");
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                logger.error("Error while creating file " + app + "_actions.csv", ioe);
                return;
            }
            try (FileWriter writer = new FileWriter(app + "_actions.csv")) {
                writer.write(header.toString() + "\n");
            } catch (IOException ioe) {
                logger.error("Error while writing file " + app + "_actions.csv", ioe);
                return;
            }
        }
        List<String> newMetrics = measurements.stream()
                .map(Metric::getName)
                .filter(name -> !knownMetricsAction.get(app).contains(name))
                .collect(Collectors.toList());
        if (!newMetrics.isEmpty()) {
            newMetrics.forEach(name -> {
                logger.warn("A new metric appeared: " + name + ". Currently unable to handle this");
                // knownMetricsAction.get(app).add(name);
                // append name to file header
                // append ,<MISSINGVALUESTRATEGY.DEFAULTVALUE> to each line in file
            });
        }
        StringBuilder line = new StringBuilder();
        line.append(time).append(",").append(action).append(",");
        for (String metricName : knownMetricsAction.get(app)) {
            OptionalDouble value = measurements.stream()
                    .filter(metric -> metric.getName().equals(metricName))
                    .mapToDouble(Metric::getValue)
                    .findFirst();
            if (value.isPresent() && !Double.isNaN(value.getAsDouble())) {
                line.append(value.getAsDouble()).append(",");
            } else {
                line.append(missingValueStrategy.getDefaultValue());
                line.append(",");
            }
        }
        // Delete last comma
        line.deleteCharAt(line.length() - 1);
        try (FileWriter writer = new FileWriter(app + "_actions.csv", true)) {
            writer.write(line.toString() + "\n");
        } catch (IOException ioe) {
            logger.error("Error while writing file " + app + "_actions.csv", ioe);
        }
    }

    @Override
    public void commitMeasurements(long time, List<Metric> measurements) {
        Set<String> apps = measurements.stream().map(Metric::getApp).collect(Collectors.toSet());
        for (String app : apps) {
            List<Metric> measurementsOfApp = measurements.stream()
                    .filter(metric -> metric.getApp().equals(app))
                    .collect(Collectors.toList());
            writeToMeasurementFile(app, time, measurementsOfApp);
        }
    }

    private void writeToMeasurementFile(String app, long time, List<Metric> measurements) {
        if (!knownMetrics.containsKey(app)) {
            StringBuilder header = new StringBuilder();
            header.append("time");
            knownMetrics.put(app, new ArrayList<>());
            measurements.stream().map(Metric::getName).forEach(name -> {
                header.append(",").append(name);
                knownMetrics.get(app).add(name);
            });
            File file = new File(app + ".csv");
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                logger.error("Error while creating file " + app + ".csv", ioe);
                return;
            }
            try (FileWriter writer = new FileWriter(app + ".csv")) {
                writer.write(header.toString() + "\n");
            } catch (IOException ioe) {
                logger.error("Error while writing file " + app + ".csv", ioe);
                return;
            }
        }
        List<String> newMetrics = measurements.stream()
                .map(Metric::getName)
                .filter(name -> !knownMetrics.get(app).contains(name))
                .collect(Collectors.toList());
        if (!newMetrics.isEmpty()) {
            newMetrics.forEach(name -> {
                logger.warn("A new metric appeared: " + name + ". Currently unable to handle this");
                // knownMetrics.get(app).add(name);
                // append name to file header
                // append ,<MISSINGVALUESTRATEGY.DEFAULTVALUE> to each line in file
            });
        }
        StringBuilder line = new StringBuilder();
        line.append(time).append(",");
        for (String metricName : knownMetrics.get(app)) {
            OptionalDouble value = measurements.stream()
                    .filter(metric -> metric.getName().equals(metricName))
                    .mapToDouble(Metric::getValue)
                    .findFirst();
            if (value.isPresent() && !Double.isNaN(value.getAsDouble())) {
                line.append(value.getAsDouble()).append(",");
            } else {
                line.append(missingValueStrategy.getDefaultValue());
                line.append(",");
            }
        }
        // Delete last comma
        line.deleteCharAt(line.length() - 1);
        try (FileWriter writer = new FileWriter(app + ".csv", true)) {
            writer.write(line.toString() + "\n");
        } catch (IOException ioe) {
            logger.error("Error while writing file " + app + ".csv", ioe);
        }
    }
}
