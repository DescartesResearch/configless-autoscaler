package tools.descartes.autoscaling.dummy.scaling;

import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.scaling.appscaler.ReactiveSingleAppScaler;
import tools.descartes.autoscaling.dummy.scaling.appscaler.SingleAppScaler;

/**
 * A child class of MultiAppScaler which instantiates reactive single app scalers.
 */
@Component
public class ReactiveScaler extends MultiAppScaler {

	public ReactiveScaler(ScalingProperties scalingProperties, InstanceManager manager) {
		super(manager, scalingProperties);
	}

	@Override
	public SingleAppScaler createAppScaler(String app) {
		return new ReactiveSingleAppScaler(app, scalingProperties, instanceManager);
	}
}
