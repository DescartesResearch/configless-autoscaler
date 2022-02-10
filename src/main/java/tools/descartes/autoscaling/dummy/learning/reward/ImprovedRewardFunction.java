package tools.descartes.autoscaling.dummy.learning.reward;

import tools.descartes.autoscaling.dummy.metrics.Metric;

import java.util.List;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

public class ImprovedRewardFunction implements IRewardFunction {

	private static final double MAX_INSTANCES = 10;

	private static final double WEIGHT = 0.9;

	private static final double STEEPNESS = 2;

	@Override
	public double getReward(double instances, List<Metric> kpiMetrics, List<KPIProperties> kpis) {
		double totalReward = 0;
		int kpiFound = 0;
		for (Metric m : kpiMetrics) {
			for (KPIProperties kpiInfo : kpis) {
				if (kpiInfo.getName().equals(m.getName())) {
					totalReward += getRewardOfOneKPI(instances, m.getValue(), kpiInfo.getSlo());
					kpiFound++;
					break;
				}
			}
		}
		return totalReward / kpiFound;
	}

	private double getRewardOfOneKPI(double instances, double kpiVal, double kpiTarget) {
		// scale the instance value, when
		double instanceValue = instances / MAX_INSTANCES;

		// if x is smaller than 1, we have no problem with our Kpis -> kpiValue should
		// be 0
		double x = Math.max((kpiVal / kpiTarget) - 1, 0);

		double kpiValue = (STEEPNESS * x) / (STEEPNESS * x + 1);

		double target = (1 - WEIGHT) * instanceValue + WEIGHT * kpiValue;

		return -target;
	}

}
