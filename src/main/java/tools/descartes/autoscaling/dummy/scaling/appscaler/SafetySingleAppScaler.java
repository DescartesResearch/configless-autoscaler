package tools.descartes.autoscaling.dummy.scaling.appscaler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * A safety scaler for a single app
 */
public class SafetySingleAppScaler extends SingleAppScaler {

	private final Logger logger = LoggerFactory.getLogger(SafetySingleAppScaler.class);
	private int currInterval;
	private final int period;
	private final double ratio;

	/**
	 * Maps from an interval number (Integer) to an internal data structure
	 * (MeasurementsStore) for measurements of that interval.
	 */
	private final Map<Integer, MeasurementsStore> intervalMapping;

	public SafetySingleAppScaler(String app, ScalingProperties scalingProperties, InstanceManager manager) {
		super(app, scalingProperties, manager);
		currInterval = 0;
		this.period = extractPeriod();
		this.ratio = extractRatio();
		intervalMapping = new HashMap<>();
	}

	private int extractPeriod() {
	    int configPeriod = scalingProperties.getSafety().getPeriod();
		if (configPeriod == Integer.MIN_VALUE) {
			logger.warn("No period value was configured for the safety scaler. Deactivating safety scaler for app: \""
							+ app + "\".");
			return 0;
		} else if (configPeriod <= 0) {
				logger.warn("The period value (" + configPeriod
						+ ") given to the saftey scaler is not supported. Deactivating safety scaler for app: \""
						+ app + "\".");
				return 0;
		}
		return configPeriod;
	}

	private double extractRatio() {
		double ratio = scalingProperties.getSafety().getRatio();
		if (ratio == Double.MIN_VALUE) {
			getLogger()
					.info("No ratio value was configured for the saftey scaler. Choosing default ratio of 1 for app: \""
							+ app + "\".");
			return 1;
		} else if (ratio <= 0 || ratio > 1) {
				logger.warn("The given ratio (" + ratio
						+ ") given to the saftey scaler is not supported. Choosing default ratio of 1 for app: \""
						+ app+ "\".");
				return 1;
		}
		return ratio;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public List<ScalingRequest> calculateScalingRequests(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis, int lastAction) {
		List<ScalingRequest> list = new ArrayList<>();
		if (isActive()) {
			MeasurementsStore interval = addToHistory(measurements, kpiMetrics, kpis);
			int minInstances = interval.retrieveMinValueFromHistory();
			int targetinstances = (int) (minInstances / ratio);
			if (instanceManager.getCountOf(app) < targetinstances) {
				logger.info("Safety scaler for app \"" + app + "\" determines a minimum number of " + targetinstances
						+ " necessary and therefore issues a scaling request to match at least this number.");
				list.add(new ScalingRequest(app, targetinstances));
			}
		}
		return list;
	}

	private void updateInterval() {
		// currently we do one interval per measurement
		currInterval = (currInterval + 1) % period;
	}

	private MeasurementsStore addToHistory(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis) {
		updateInterval();
		MeasurementsStore store = intervalMapping.computeIfAbsent(currInterval, k -> new MeasurementsStore());
		if (!isAnySLOViolated(kpiMetrics, kpis)) {
			// we only consider the example, if the kpi is fine
			store.addExample(measurements, instanceManager.getCountOf(app));
		}
		return store;
	}

	private boolean isActive() {
		// only do scaling if active (i.e., if period has a meaningful value)
		return period > 0;
	}

	static class MeasurementsStore {

		List<Integer> minInstances;

		public MeasurementsStore() {
			minInstances = new ArrayList<>();
		}

		private int retrieveMinValueFromHistory() {
			int minval = Integer.MAX_VALUE;
			for (Integer integer : minInstances) {
				minval = Math.min(minval, integer);
			}
			if (minval != Integer.MAX_VALUE) {
				return minval;
			}
			return 0;
		}

		public void addExample(List<Metric> measurements, int currentInstances) {
			// currently, we ignore all metrics, as we are just interested in the current
			// number of running instances
			minInstances.add(currentInstances);
		}

	}
}
