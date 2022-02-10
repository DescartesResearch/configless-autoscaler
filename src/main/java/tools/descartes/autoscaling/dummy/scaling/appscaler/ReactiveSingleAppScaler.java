package tools.descartes.autoscaling.dummy.scaling.appscaler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * A reactive scaler for a single app using Kubernetes default scaling rule
 */
public class ReactiveSingleAppScaler extends SingleAppScaler {

	private final Logger logger = LoggerFactory.getLogger(ReactiveSingleAppScaler.class);

	public ReactiveSingleAppScaler(String app, ScalingProperties scalingProperties, InstanceManager manager) {
		super(app, scalingProperties, manager);
	}

	@Override
	public List<ScalingRequest> calculateScalingRequests(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis, int lastAction) {
		List<ScalingRequest> list = new ArrayList<>();
		if (isAnySLOViolated(kpiMetrics, kpis)) {
			// currently we only support MAX_KPIs, aka only define upper bounds
			int currentInstances = instanceManager.getCountOf(app);
			// we check all kpis, do linear interpolation and choose the max number
			int maxInstances = Integer.MIN_VALUE;
			Metric decisionMetric = null;
			double decisionSLO = Double.MIN_VALUE;
			for (Metric m : kpiMetrics) {
				for (KPIProperties kpi : kpis) {
					if (kpi.getName().equals(m.getName())) {
						if (isSLOViolated(m, kpi.getSlo())) {
							// currently, we only support linear interpolation
							int newInstances = getLinearInterpolation(currentInstances, m.getValue(), kpi.getSlo());
							if (newInstances > maxInstances) {
								maxInstances = newInstances;
								decisionMetric = m;
								decisionSLO = kpi.getSlo();
							}
						}
						break;
					}
				}
			}

			list.add(new ScalingRequest(app, maxInstances));
			logger.info("Reactive scaler for app \"" + app + "\": Measured KPI (\"" + decisionMetric.getName()
					+ "\") value of " + decisionMetric.getValue() + " with defined SLO of " + decisionSLO
					+ ". Increasing instances (linear policy) from " + currentInstances + " to " + maxInstances + ".");
		}
		return list;
	}

	private int getLinearInterpolation(int currentInstances, double value, double kpiSLO) {
		if (value <= kpiSLO) {
			throw new IllegalArgumentException("Not prepared for linear scaling if value is smaller than target.");
		}
		return (int) Math.ceil(currentInstances * (value / kpiSLO));
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
