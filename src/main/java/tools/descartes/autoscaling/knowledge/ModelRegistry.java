package tools.descartes.autoscaling.knowledge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.learning.IRLModel;

/**
 * Registry for each App, that keeps an up-to-date ML model in memory.
 */
@Component
public class ModelRegistry {

	private final Map<String, IRLModel> registry;

	public ModelRegistry() {
		registry = new ConcurrentHashMap<>();
	}

	/**
	 * Gets a {@link IRLModel} instance for each registered app, and returns
	 * an up-to-date model.
	 * 
	 * @param app The string code of the requested model app.
	 * @return The model for this app or null if not present
	 */
	public IRLModel getModelForApp(String app) {
		return registry.get(app);
	}

	public void setModelForApp(String app, IRLModel model) {
		registry.put(app, model);
	}

}
