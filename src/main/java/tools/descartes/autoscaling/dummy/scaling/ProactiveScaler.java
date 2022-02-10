package tools.descartes.autoscaling.dummy.scaling;

import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.dummy.learning.reward.RewardFunctionWrapper;
import tools.descartes.autoscaling.dummy.scaling.appscaler.ProactiveSingleAppScaler;
import tools.descartes.autoscaling.dummy.scaling.appscaler.SingleAppScaler;
import tools.descartes.autoscaling.dummy.knowledge.ModelRegistry;

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
