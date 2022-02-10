package tools.descartes.autoscaling.dummy.scaling;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.dummy.cloudfoundry.CFConnector;
import tools.descartes.autoscaling.dummy.cloudfoundry.CFConnectorWrapper;
import tools.descartes.autoscaling.dummy.config.*;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataExporter;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataExporterWrapper;
import tools.descartes.autoscaling.dummy.metrics.*;

import static tools.descartes.autoscaling.dummy.config.RulesProperties.ScalingExpression;

/**
 * The core class which runs the autoscaling and measurement queries
 */
@Component
public class Scaler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Scaler.class);

    private final AtomicBoolean isRunning;
    private final InstanceManager instanceManager;
    private final ProactiveScaler proactiveScaler;
    private final ReactiveScaler reactiveScaler;
    private final SafetyScaler safetyScaler;
    private final CFConnector cfConnector;
    private final TrainingDataExporter trainingDataExporter;
    private final PrometheusProperties prometheusProperties;
    private final TrainingProperties trainingProperties;
    private final ScalingProperties scalingProperties;
    private final RulesProperties rulesProperties;
    private final MetricsProperties metricsProperties;
    private final RuntimeDataImporter runtimeDataImporter;
    private long scalingAllowedAtTime = 0;
    private final Map<String, Long> upscaleTime;
    private final Map<String, Long> downscaleTime;

    public Scaler(InstanceManager instanceManager, RulesProperties rulesProperties,
                  CFConnectorWrapper cfConnector, ProactiveScaler proactiveScaler, ReactiveScaler reactiveScaler,
                  SafetyScaler safetyScaler, TrainingDataExporterWrapper trainingExporter,
                  PrometheusProperties prometheusProperties, TrainingProperties trainingProperties,
                  ScalingProperties scalingProperties, MetricsProperties metricsProperties,
                  RuntimeDataImporterWrapper runtimeDataImporterWrapper) {
        isRunning = new AtomicBoolean(false);
        this.instanceManager = instanceManager;
        this.proactiveScaler = proactiveScaler;
        this.reactiveScaler = reactiveScaler;
        this.safetyScaler = safetyScaler;
        this.cfConnector = cfConnector.getCfConnector();
        this.trainingDataExporter = trainingExporter.getExporter();
        this.runtimeDataImporter = runtimeDataImporterWrapper.getRuntimeDataImporter();
        upscaleTime = new HashMap<>();
        downscaleTime = new HashMap<>();
        this.prometheusProperties = prometheusProperties;
        this.trainingProperties = trainingProperties;
        this.scalingProperties = scalingProperties;
        this.rulesProperties = rulesProperties;
        this.metricsProperties = metricsProperties;
    }

    @Override
    public void run() {
        isRunning.set(true);
        // First, we initialize the InstanceManager which is required for scaling
        instanceManager.init();
        boolean exportData = trainingProperties.getExporter().isEnabled();
        Map<String, Integer> lastActions = new HashMap<>();
        long currentTime;
        while (isRunning.get()) {
            currentTime = System.currentTimeMillis();
            // Collect measured data for export (if training data without actions should be exported)
            if (exportData) {
                logger.info("Exporting measurement data for training");
                List<Metric> data = importMeasurementData(false);
                trainingDataExporter.commitMeasurements(currentTime, data);
            }
            // If autoscaling logic can be triggered (no timeout active)
            if (currentTime >= scalingAllowedAtTime) {
                // Import measurement data
                List<Metric> aggregatedData = importMeasurementData(true);
                // Calculate scaling requests
                List<ScalingRequest> requests = calculateScaling(aggregatedData, lastActions);
                // Enforce logical constraints
                requests = enforceLogicalConstraints(requests);
                // Enforce user set constraints
                requests = enforceConstraints(requests);
                if (requests.isEmpty()) logger.info("No scaling action needed");
                // Set new scaling timeout
                scalingAllowedAtTime = currentTime + (long) scalingProperties.getTimeout() * 1000;
                Map<String, Integer> executedActions = new HashMap<>();
                // Execute scaling actions
                for (ScalingRequest request : requests) {
                    ScalingResponse response = executeScaling(request);
                    if (response.isSuccess()) {
                        logger.info("Scaling action \"" + request.toString() + "\" successful");
                        int diff = instanceManager.setCountOf(request.getApp(), request.getInstances());
                        executedActions.put(request.getApp(), diff);
                    } else {
                        logger.error("Scaling action \"" + request.toString() + "\" failed, reason: " + response.getMessage());
                        executedActions.put(request.getApp(), 0);
                    }
                }
                // Export actions and measurement data which led to these actions
                if (exportData) {
                    logger.info("Exporting action data for training");
                    Set<String> apps = aggregatedData.stream().map(Metric::getApp).collect(Collectors.toSet());
                    // Complete actions
                    apps.forEach(app -> {
                        if (!executedActions.containsKey(app)) executedActions.put(app, 0);
                    });
                    trainingDataExporter.commitActions(currentTime, aggregatedData, executedActions);
                }
                lastActions = executedActions;
            }
            // Depending on whether measurements without actions should be exported, set timeout and sleep
            int timeoutSeconds = scalingProperties.getTimeout();
            int exportTimeoutSeconds = prometheusProperties.getScrapeDuration();
            try {
                int finalTimeout = timeoutSeconds < exportTimeoutSeconds ? timeoutSeconds * 1000 : exportTimeoutSeconds * 1000;
                Thread.sleep(finalTimeout);
            } catch (InterruptedException ie) {
                logger.error("InterruptedException while scaling pause");
            }
        }
    }

    public void exit() {
        isRunning.set(false);
    }

    private List<Metric> importMeasurementData(boolean aggregate) {
        int scalingWindowSeconds;
        if (aggregate) {
            scalingWindowSeconds = scalingProperties.getTimeout();
        } else {
            scalingWindowSeconds = prometheusProperties.getScrapeDuration();
        }
        String scalingMetric = metricsProperties.getIncluderegex();
        List<Metric> result;
        List<String> metricNames;
        if (scalingMetric == null) {
            metricNames = runtimeDataImporter.getAllMetricNames();
            result = new ArrayList<>();
            for (String metric : metricNames) {
                result.addAll(runtimeDataImporter.getAveragedValuesForMetric(metric, scalingWindowSeconds));
            }
        } else {
            Pattern includePattern = Pattern.compile(scalingMetric);
            metricNames = runtimeDataImporter.getAllMetricNames();
            result = new ArrayList<>();
            for (String metric : metricNames) {
                if (includePattern.matcher(metric).find()) {
                    result.addAll(runtimeDataImporter.getAveragedValuesForMetric(metric, scalingWindowSeconds));
                }
            }
        }
        Map<String, List<ScalingProperties.KPIProperties>> kpis = scalingProperties.getKpis();
        for (String app : kpis.keySet()) {
            List<ScalingProperties.KPIProperties> kpisForApp = kpis.get(app);
            for (ScalingProperties.KPIProperties kpi : kpisForApp) {
                if (!metricNames.contains(kpi.getName())) {
                    RewritableMetric manual = runtimeDataImporter.getSingleMetric(kpi.getQuery());
                    if (manual == null) continue;
                    manual.setName(kpi.getName());
                    manual.setApp(app);
                    result.add(manual);
                }
            }
        }
        return result;
    }

    private List<ScalingRequest> calculateScaling(List<Metric> data, Map<String, Integer> lastActions) {
        List<ScalingRequest> safetyRequests = safetyScaler.calculateScaling(data, lastActions);
        List<ScalingRequest> reactiveRequests = reactiveScaler.calculateScaling(data, lastActions);
        List<ScalingRequest> proactiveRequests = proactiveScaler.calculateScaling(data, lastActions);

        // we start with the least priority (proactive)
        List<ScalingRequest> finalScaling = new ArrayList<>(proactiveRequests);

        mergeRequests(finalScaling, reactiveRequests, "reactive");
        mergeRequests(finalScaling, safetyRequests, "safety");

        return finalScaling;
    }

    /**
     * Merges the requests of one scaler with another.
     *
     * @param finalScaling  The starting list, i.e., the default scaling.
     * @param mergeRequests The requests to merge into the starting list.
     * @param logName       The name of the scaler that created the requests to be
     *                      merged. Is just used for io.
     */
    private void mergeRequests(List<ScalingRequest> finalScaling, List<ScalingRequest> mergeRequests,
                               String logName) {
        for (ScalingRequest request : mergeRequests) {
            // all reactive requests overwrite the proactive requests, if they are higher
            // (or get added if not previously present)
            boolean match = false;
            for (ScalingRequest finalrequest : finalScaling) {
                if (finalrequest.getApp().equals(request.getApp())) {
                    // we found a match
                    match = true;
                    if (finalrequest.getInstances() < request.getInstances()) {
                        logger.info("Overwriting scaling of " + finalrequest.getInstances() + " instances by " + logName
                                + " scaler (" + request.getInstances() + " instances) for app \"" + request.getApp()
                                + "\".");
                        finalScaling.remove(finalrequest);
                        finalScaling.add(request);
                    }
                    break;
                }
            }
            if (!match) {
                // we did not find a match -> we need to insert the new scaling
                String capitalized = logName.substring(0, 1).toUpperCase() + logName.substring(1);
                logger.info(capitalized + " scaler applied to App \"" + request.getApp()
                        + "\" by setting instance count to " + request.getInstances() + ".");
                finalScaling.add(request);
            }
        }
    }

    private List<ScalingRequest> enforceLogicalConstraints(List<ScalingRequest> requests) {
        List<ScalingRequest> result = new ArrayList<>();
        for (ScalingRequest sr : requests) {
            if (sr.getInstances() <= 0) {
                logger.info("Logical constraint enforced: Request \"" + sr.toString() + "\" instance count is too low");
                ScalingRequest newSr = new ScalingRequest(sr.getApp(), 1);
                result.add(newSr);
            } else {
                result.add(sr);
            }
        }
        return result;
    }

    private List<ScalingRequest> enforceConstraints(List<ScalingRequest> requests) {
        List<ScalingRequest> result = new ArrayList<>();
        // Enforce user set constraints
        RequestLoop:
        for (ScalingRequest sr : requests) {
            List<ScalingExpression> expressions = rulesProperties.getRules().stream()
                    .filter(se -> se.getApp().equals(sr.getApp()))
                    .collect(Collectors.toList());
            for (ScalingExpression se : expressions) {
                if (!se.evaluate(sr.getInstances())) {
                    result.add(new ScalingRequest(sr.getApp(), se.getClosestValue()));
                    continue RequestLoop;
                }
            }
            result.add(sr);
        }

        List<ScalingRequest> acceptedRequests = new ArrayList<>();
        for (ScalingRequest sr : result) {
            String app = sr.getApp();
            int desiredReplicas = sr.getInstances();
            int currentReplicas = instanceManager.getCountOf(app);
            if (desiredReplicas > currentReplicas) {
                // Check upscale timing
                if (upscaleTime.get(app) == null || upscaleTime.get(app) < System.currentTimeMillis()) {
                    int usTimeout = scalingProperties.getUpscale().getTimeout();
                    upscaleTime.put(app, System.currentTimeMillis() + usTimeout * 1000L);
                } else {
                    logger.info("Upscaling for app " + app + " not allowed, timeout active");
                    continue;
                }
                // Check upscale limit
                int usLimit = scalingProperties.getUpscale().getLimit();
                if (desiredReplicas - currentReplicas > usLimit) {
                    logger.info("Upscale limit for app " + app + " enforced");
                    desiredReplicas = currentReplicas + usLimit;
                }
                acceptedRequests.add(new ScalingRequest(app, desiredReplicas));
            } else if (desiredReplicas < currentReplicas) {
                // Check downscale timing
                if (downscaleTime.get(app) == null || downscaleTime.get(app) < System.currentTimeMillis()) {
                    int dsTimeout = scalingProperties.getDownscale().getTimeout();
                    downscaleTime.put(app, System.currentTimeMillis() + dsTimeout * 1000L);
                } else {
                    logger.info("Downscaling for app " + app + " not allowed, timeout active");
                    continue;
                }
                // Check downscale limit
                int dsLimit = scalingProperties.getDownscale().getLimit();
                if (currentReplicas - desiredReplicas > dsLimit) {
                    logger.info("Downscale limit for app " + app + " enforced");
                    desiredReplicas = currentReplicas - dsLimit;
                }
                acceptedRequests.add(new ScalingRequest(app, desiredReplicas));
            }
        }
        return acceptedRequests;
    }

    private ScalingResponse executeScaling(ScalingRequest request) {
        return cfConnector.scaleApp(request);
    }
}
