package tools.descartes.autoscaling.dummy.learning;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.descartes.autoscaling.dummy.knowledge.ModelRegistry;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;
import tools.descartes.autoscaling.dummy.learning.actionspace.ActionSpace;
import tools.descartes.autoscaling.dummy.learning.reward.IRewardFunction;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * Provides comfortable access to a model for a specific app
 */
public class ReinforcementLearner {

	private final Logger logger = LoggerFactory.getLogger(ReinforcementLearner.class);
	private final ModelRegistry modelRegistry;
	private final String app;
	private final ActionSpace actions;
	private final IRewardFunction reward;

	public ReinforcementLearner(String app, ModelRegistry modelRegistry, IRewardFunction reward) {
		this.app = app;
		this.modelRegistry = modelRegistry;
		this.actions = ActionSpace.getDefaultActionSpace();
		this.reward = reward;
	}

	/**
	 *
	 * @return true if a model is trained for this app, false if not
	 */
	public boolean isTrained() {
		IRLModel model = modelRegistry.getModelForApp(app);
		return model != null;
	}

	/**
	 * Returns scaling decisions for current measurements
	 * @param measurements current measurements for this app
	 * @param kpiMetrics current KPI measurements for this app
	 * @param kpis KPIs and SLOs
	 * @param instances number of currently deployed instances
	 * @return a list containing one ScalingRequest or an empty list if error or no action needed
	 */
	public List<ScalingRequest> makePrediction(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis, int instances) {
		if (!isTrained()) {
			throw new IllegalStateException("The model of app \"" + app + "\" not yet ready for predictions...");
		}
		List<Metric> measurementsWithReward = new ArrayList<>(measurements);
		measurementsWithReward.add(Metric.of("Reward", reward.getReward(instances, kpiMetrics, kpis), app));
		IRLModel model = modelRegistry.getModelForApp(app);
		int action = model.predictBestAction(measurementsWithReward, actions);
		logger.info("The model predicted an action of  \"" + action + "\" for app\"" + app
				+ "\". Resulting target instances: " + (instances + action));
		List<ScalingRequest> list = new ArrayList<>();
		if (action != 0) {
			list.add(new ScalingRequest(app, instances + action));
		}
		return list;
	}
}
