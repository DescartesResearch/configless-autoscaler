package tools.descartes.autoscaling.dummy.scaling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.scaling.appscaler.SingleAppScaler;

/**
 * An entity which contains references to SingleAppScalers and instantiates them for all apps.
 * Moreover, it splits the measurements into different apps.
 */
@Component
public abstract class MultiAppScaler implements ScalingLogic {

	protected final ScalingProperties scalingProperties;
	protected final InstanceManager instanceManager;
	private final Map<String, SingleAppScaler> appscalers;

	public MultiAppScaler(InstanceManager manager, ScalingProperties scalingProperties) {
		this.scalingProperties = scalingProperties;
		// check if KPI definitions are present
		if (scalingProperties.getKpis() == null || scalingProperties.getKpis().isEmpty()) {
			throw new IllegalStateException("No config values found.");
		}
		this.instanceManager = manager;
		appscalers = new HashMap<>();
	}

	@Override
	public List<ScalingRequest> calculateScaling(List<Metric> measurements, Map<String, Integer> lastActions) {
		List<ScalingRequest> result = new ArrayList<>();
		// group measurements by app
		Map<String, List<Metric>> groupedByApp = measurements.stream().collect(Collectors.groupingBy(Metric::getApp));
		for (Entry<String, List<Metric>> entry : groupedByApp.entrySet()) {
			String appString = entry.getKey();
			// get or create the respective app scaler, if not already present
			SingleAppScaler appscaler = appscalers.computeIfAbsent(appString, this::createAppScaler);
			// sum up all scalings created by calling each sub-scaler for each specific app
			result.addAll(appscaler.calculateScaling(entry.getValue(), lastActions));
		}
		return result;
	}

	public abstract SingleAppScaler createAppScaler(String app);

}
