package tools.descartes.autoscaling.dummy.learning.reward;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.metrics.Metric;

import java.util.List;

public class SimpleRewardFunction implements IRewardFunction {

	@Override
	public double getReward(double instances, List<Metric> kpiMetrics, List<ScalingProperties.KPIProperties> kpis) {
		double totalReward = 0;
		for (Metric m : kpiMetrics) {
			for (ScalingProperties.KPIProperties kpiInfo : kpis) {
				if (kpiInfo.getName().equals(m.getName())) {
					totalReward += getRewardOfOneKPI(instances, m.getValue(), kpiInfo.getSlo());
					break;
				}
			}
		}
		return totalReward;
	}

	private double getRewardOfOneKPI(double instances, double kpiVal, double kpiTarget) {
		// Assuming 0 is an invalid measurement
		/*
		if (kpiVal == 0) {
			kpiVal = 1.001 * kpiTarget;
		}
		 */
		double value = -instances;

		if (kpiVal <= kpiTarget) {
			return value;
		}

		return (kpiVal / kpiTarget) * value;
	}

}
