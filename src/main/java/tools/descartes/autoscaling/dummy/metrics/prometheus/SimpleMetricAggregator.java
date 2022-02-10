package tools.descartes.autoscaling.dummy.metrics.prometheus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.RewritableMetric;
import tools.descartes.autoscaling.dummy.metrics.SimpleMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static tools.descartes.autoscaling.dummy.metrics.prometheus.MatrixResult.MatrixResultData.MatrixEntry;

/**
 * A MetricAggregator which averages black-box metrics
 */
public class SimpleMetricAggregator implements MetricAggregator {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMetricAggregator.class);

    private final Pattern includePattern;
    private final Pattern excludePattern;

    public SimpleMetricAggregator(String includeRegex, String excludeRegex) {
        includePattern = Pattern.compile(includeRegex);
        excludePattern = Pattern.compile(excludeRegex);
    }

    @Override
    public RewritableMetric aggregateMetrics(VectorResult vector) {
        if (vector.data.result.isEmpty()) {
            logger.warn("VectorResult has no data");
            return null;
        }
        double metricValue = vector.data.result.stream()
                .mapToDouble(entry -> Double.parseDouble(entry.value.get(1).toString()))
                .average()
                .orElse(Double.MIN_VALUE);
        if (metricValue == Double.MIN_VALUE) {
            logger.error("Aggregation of VectorResult failed");
            return null;
        }
        return new SimpleMetric(null, null, metricValue);
    }

    @Override
    public List<Metric> aggregateMetrics(MatrixResult matrix) {
        if (matrix.data.result.isEmpty()) {
            logger.warn("MatrixResult has no data");
            return new ArrayList<>();
        }
        String metricName = matrix.data.result.stream().findFirst().get().metric.get("__name__");
        Map<String, List<Double>> tempMap = new HashMap<>();
        for (MatrixEntry entry : matrix.data.result) {
            String app = entry.metric.get("app");
            if (app == null) {
                logger.trace("Found metric " + metricName + " without app label");
                continue;
            } else if (!(includePattern.matcher(app).find() && !excludePattern.matcher(app).find())) {
                logger.trace("Ignoring metric " + metricName + ": app " + app + " excluded");
                continue;
            }
            // Average values over time
            double average = entry.values.stream()
                    .mapToDouble(list -> Double.parseDouble(list.get(1).toString()))
                    .average()
                    .orElse(Double.MIN_VALUE);
            if (average == Double.MIN_VALUE) {
                logger.warn("MatrixEntry could not be averaged: " + entry.toString());
            }
            if (tempMap.containsKey(app)) {
                tempMap.get(app).add(average);
            } else {
                List<Double> list = new ArrayList<>();
                list.add(average);
                tempMap.put(app, list);
            }
        }
        List<Metric> result = new ArrayList<>();
        for (String app : tempMap.keySet()) {
            double average = tempMap.get(app).stream().mapToDouble(d -> d).average().getAsDouble();
            SimpleMetric metric = new SimpleMetric(metricName, app, average);
            result.add(metric);
        }
        return result;
    }
}
