package tools.descartes.autoscaling.scaling;

import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.config.ScalingProperties;
import tools.descartes.autoscaling.knowledge.InstanceManager;
import tools.descartes.autoscaling.scaling.appscaler.SafetySingleAppScaler;
import tools.descartes.autoscaling.scaling.appscaler.SingleAppScaler;

/**
 * A child class of MultiAppScaler which instantiates safety single app scalers.
 */
@Component
public class SafetyScaler extends MultiAppScaler {

	public SafetyScaler(ScalingProperties scalingProperties, InstanceManager manager) {
		super(manager, scalingProperties);
	}

	@Override
	public SingleAppScaler createAppScaler(String app) {
		return new SafetySingleAppScaler(app, scalingProperties, instanceManager);
	}
}
