package tools.descartes.autoscaling.dummy.learning.reward;

import tools.descartes.autoscaling.dummy.metrics.Metric;

import java.util.List;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * Value or reward function for a given state.
 */
@FunctionalInterface
public interface IRewardFunction {

	/**
	 * Calculates the reward or value function for a given scenario, depending on
	 * the number of instances, the KPI measurement and the KPI target.
	 * 
	 * @param instances The current amount of live instances. (Relevant for the
	 *                  cost.)
	 * @param kpiMetrics   The current values for the KPIs.
	 * @param kpis KPIProperties containing thresholds for each KPI.
	 * @return The calculated value of the given state.
	 */
	 double getReward(double instances, List<Metric> kpiMetrics, List<KPIProperties> kpis);

}
