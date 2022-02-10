package tools.descartes.autoscaling.dummy.scaling.appscaler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.ScalingLogic;
import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * This abstract class implements the interface {@link ScalingLogic}. However,
 * its implementation is more strict than the interface, as it accepts only
 * filtered data from one app.
 */
public abstract class SingleAppScaler implements ScalingLogic {

	protected final String app;
	protected final ScalingProperties scalingProperties;
	protected final InstanceManager instanceManager;

	public SingleAppScaler(String app, ScalingProperties scalingProperties, InstanceManager manager) {
		this.app = app;
		this.scalingProperties = scalingProperties;
		this.instanceManager = manager;
	}

	/**
	 * Returns scaling decisions for measurements. Measurements must not contain measurements for other apps.
	 * Returns empty list if KPI measurements are not present.
	 * @param measurements current measurements
	 * @param lastActions last executed actions (might be used by proactive scaling to update backoff)
	 * @return scaling decisions
	 */
	@Override
	public List<ScalingRequest> calculateScaling(List<Metric> measurements, Map<String, Integer> lastActions) {
		List<KPIProperties> kpis = scalingProperties.getKpis().get(app);
		if (kpis == null || kpis.isEmpty()) {
			getLogger().warn("No config values found. Ignoring scaling for App \"" + app + "\".");
			return new ArrayList<>();
		}
		List<Metric> kpiMetrics = new ArrayList<>();
		int foundKPIs = 0;
		for (Metric m : measurements) {
			if (!m.getApp().equals(app)) {
				throw new IllegalArgumentException("This Appscaler supports only App: \"" + app
						+ "\". At least one given metric was about \"" + m.getApp() + "\".");
			}
			for (KPIProperties kpi : kpis) {
				if (kpi.getName().equals(m.getName())) {
					foundKPIs++;
					kpiMetrics.add(m);
					break;
				}
			}
		}
		if (foundKPIs != kpis.size()) {
			// TODO this might be changed in the future
			getLogger().warn(
					"Could not find all KPI values. The current implementation does not react if no KPI measurements are present. Ignoring scaling for App \""
							+ app + "\". Expected " + kpis.size() + " but got " + foundKPIs);
			return new ArrayList<>();
		} else {
			return calculateScalingRequests(measurements, kpiMetrics, kpis, lastActions.getOrDefault(app, Integer.MAX_VALUE));
		}
	}

	protected abstract List<ScalingRequest> calculateScalingRequests(List<Metric> measurements, List<Metric> kpiMetrics,
			List<KPIProperties> kpis, int lastAction);

	public boolean isAnySLOViolated(List<Metric> kpiMetrics, List<KPIProperties> kpis) {
		for (Metric kpiMeasurement : kpiMetrics) {
			for (KPIProperties kpiInfo : kpis) {
				if (kpiMeasurement.getName().equals(kpiInfo.getName())) {
					if (isSLOViolated(kpiMeasurement, kpiInfo.getSlo())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isSLOViolated(Metric kpi, Double kpiSLO) {
		// currently we only support MAX_KPIs, aka only define upper bounds
		// TODO support Min-KPIs as well
		return kpi.getValue() > kpiSLO;
	}

	protected abstract Logger getLogger();

}
