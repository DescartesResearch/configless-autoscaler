package tools.descartes.autoscaling.dummy.learning.smile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.Regression;
import tools.descartes.autoscaling.dummy.learning.IRLModel;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategy;
import tools.descartes.autoscaling.dummy.learning.actionspace.ActionSpace;

/**
 * An implementation of IRLModel which uses the SMILE library
 */
public class SMILEModel implements IRLModel {

	private static final Logger logger = LoggerFactory.getLogger(SMILEModel.class);
	private final Regression<Tuple> model;
	private final String app;
	private final StructType schema;
	private final MissingValueStrategy missingValueStrategy;

	public SMILEModel(String app, Regression<Tuple> model, StructType dataschema, MissingValueStrategy missingValueStrategy) {
		super();
		this.model = model;
		this.schema = dataschema;
		this.app = app;
		this.missingValueStrategy = missingValueStrategy;
	}

	public Regression<Tuple> getModel() {
		return model;
	}

	@Override
	public Map<Integer, Double> predictValuesPerAction(List<Metric> measurements, ActionSpace space) {
		Map<Integer, Double> result = new HashMap<>();
		for (int action : space.getActions()) {
			double prediction = model.predict(transformMeasurementsToTuple(measurements, action));
			logger.trace("App \"" + app + "\": The model predicted a value of " + prediction + " for action "
					+ action);
			result.put(action, prediction);
		}
		return result;
	}

	private Tuple transformMeasurementsToTuple(List<Metric> measurements, int action) {
		double[] result = new double[schema.length()];
		for (int i = 0; i < schema.length(); i++) {
			String field = schema.field(i).name;
			Metric m = findMeasurement(measurements, field);
			if (m != null) {
				result[i] = m.getValue();
			} else {
				// if we did not find a corresponding metric
				if (field.equals("action"))
					// we can insert the required action
					result[i] = action;
				else {
					logger.debug("Metric " + field + " is not contained in the measurements. Using MissingValueStrategy");
					result[i] = missingValueStrategy.getDefaultValue();
				}
			}
		}
		return Tuple.of(result, schema);
	}

	private Metric findMeasurement(List<Metric> measurements, String name) {
		for (Metric metric : measurements) {
			if (metric.getName().equals(name)) {
				return metric;
			}
		}
		return null;
	}

}
