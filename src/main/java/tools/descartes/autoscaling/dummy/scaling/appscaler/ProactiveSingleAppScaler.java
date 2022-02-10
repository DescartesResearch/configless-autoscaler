package tools.descartes.autoscaling.dummy.scaling.appscaler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.learning.ReinforcementLearner;
import tools.descartes.autoscaling.dummy.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.ScalingRequest;
import tools.descartes.autoscaling.dummy.knowledge.ModelRegistry;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * A proactive scaler for a single app with exponential backoff
 */
public class ProactiveSingleAppScaler extends SingleAppScaler {

	private static final int[] BACKOFF_TABLE = { 1, 2, 4, 8 };

	private final Logger logger = LoggerFactory.getLogger(ProactiveSingleAppScaler.class);
	private final ReinforcementLearner model;
	private int numFineIntervals;
	private boolean appliedDownScaling;
	private int backoffTime;

	public ProactiveSingleAppScaler(String app, ScalingProperties scalingProperties, InstanceManager manager, ModelRegistry modelRegistry,
									IRewardFunction reward) {
		super(app, scalingProperties, manager);
		this.model = new ReinforcementLearner(app, modelRegistry, reward);
		numFineIntervals = 0;
		appliedDownScaling = false;
		backoffTime = BACKOFF_TABLE[0];
	}

	@Override
	protected List<ScalingRequest> calculateScalingRequests(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis,
			int lastAction) {
		if (!model.isTrained()) {
			logger.debug("Model for app " + app + " is not trained yet, using default behavior");
			return getDefaultBehavior(measurements, kpiMetrics, kpis, lastAction);
		} else {
			logger.debug("Model for app " + app + " is trained");
			return getModelDecisionWithExploration(measurements, kpiMetrics, kpis, lastAction);
		}
	}

	private List<ScalingRequest> getModelDecisionWithExploration(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis, int lastAction) {
		// Get output from model
		List<ScalingRequest> modelOutput = model.makePrediction(measurements, kpiMetrics, kpis, instanceManager.getCountOf(app));
		Optional<ScalingRequest> reqForApp = modelOutput.stream().filter(sr -> sr.getApp().equals(app)).findFirst();
		// But also evaluate what default policy would say (keeps backoff and all other variables up to date)
		List<ScalingRequest> defaultPolicyOutput = getDefaultBehavior(measurements, kpiMetrics, kpis, lastAction);
		// if the default policy does not do anything -> we trust the model
		if (defaultPolicyOutput.isEmpty()) return modelOutput;
		// if the model returns action 0 -> we use the default policy
		if (!reqForApp.isPresent() || reqForApp.get().getInstances() == instanceManager.getCountOf(app)) {
			logger.info("Applying the explorative downscaling decision for app \"" + app + "\"");
			return defaultPolicyOutput;
		} else {
			return modelOutput;
		}
	}

	private List<ScalingRequest> getDefaultBehavior(List<Metric> measurements, List<Metric> kpiMetrics, List<KPIProperties> kpis,
			int lastAction) {
		List<ScalingRequest> list = new ArrayList<>();
		// per default, we only react, if SLOs are not violated (if they are, the
		// reactive scaler will do its part, so there is no need for action here
		if (!isAnySLOViolated(kpiMetrics, kpis)) {
			if (appliedDownScaling && lastAction == -1) {
				// if we previously successfully applied downscaling, reset the backoff
				resetBackOffTime();
			}
			appliedDownScaling = false;
			// increase the seen number of intervals than are consecutively fine
			numFineIntervals++;
			if (tryDownScaling()) {
				// we try to downscale
				appliedDownScaling = true;
				int currInstances = instanceManager.getCountOf(app);
				int newInstances = currInstances - 1;
				list.add(new ScalingRequest(app, newInstances));
			}
		} else {
			numFineIntervals = 0;
			// the SLO is currently violated, so we reset our counter
			if (appliedDownScaling && lastAction == -1) {
				// if we issued a downscaling in the last instance, we need to back-off as it
				// did not show the desired effect
				logger.info("Backing off unsuccessful downscaling for app \"" + app + "\" .");
				backOff();
			}
			appliedDownScaling = false;
		}
		return list;
	}

	private void backOff() {
		// if the backoff is already at MAX, we do not do anything
		if (backoffTime == BACKOFF_TABLE[BACKOFF_TABLE.length - 1]) {
			logger.debug("Backoff is already at max value of " + backoffTime + ".");
			return;
		}
		// this would be more efficient if we store it, but currently there is probably
		// no need for it
		for (int i = 0; i < BACKOFF_TABLE.length - 1; i++) {
			if (backoffTime == BACKOFF_TABLE[i]) {
				backoffTime = BACKOFF_TABLE[i + 1];
				logger.debug("Increasing backoff to wait for " + backoffTime + " intervals.");
				return;
			}
		}
		throw new IllegalStateException("The code is not supposed to reach here. This is probably a bug.");
	}

	private void resetBackOffTime() {
		backoffTime = BACKOFF_TABLE[0];
		logger.debug("Resetting backoff-time to wait for " + backoffTime + " intervals.");
	}

	private boolean tryDownScaling() {
		// if the number of intervals that are consecutively fine is bigger than our
		// prescribed waiting interval
		return numFineIntervals > backoffTime;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
