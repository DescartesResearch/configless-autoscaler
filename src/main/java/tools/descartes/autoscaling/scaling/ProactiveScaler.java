package tools.descartes.autoscaling.scaling;

import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.config.ScalingProperties;
import tools.descartes.autoscaling.knowledge.InstanceManager;
import tools.descartes.autoscaling.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.learning.reward.RewardFunctionWrapper;
import tools.descartes.autoscaling.scaling.appscaler.ProactiveSingleAppScaler;
import tools.descartes.autoscaling.scaling.appscaler.SingleAppScaler;
import tools.descartes.autoscaling.knowledge.ModelRegistry;

/**
 * A child class of MultiAppScaler which instantiates proactive single app scalers.
 */
@Component
public class ProactiveScaler extends MultiAppScaler {

	private final ModelRegistry modelRegistry;
	private final IRewardFunction reward;

	public ProactiveScaler(ScalingProperties scalingProperties, InstanceManager manager, ModelRegistry modelRegistry,
                           RewardFunctionWrapper rewardFunctionWrapper) {
		super(manager, scalingProperties);
		this.modelRegistry = modelRegistry;
		this.reward = rewardFunctionWrapper.getRewardFunction();
	}

	@Override
	public SingleAppScaler createAppScaler(String app) {
		return new ProactiveSingleAppScaler(app, scalingProperties, instanceManager, modelRegistry, reward);
	}
}
