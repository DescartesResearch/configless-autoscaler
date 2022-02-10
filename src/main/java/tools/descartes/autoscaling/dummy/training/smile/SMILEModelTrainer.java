package tools.descartes.autoscaling.dummy.training.smile;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.regression.Regression;
import tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;
import tools.descartes.autoscaling.dummy.learning.IRLModel;
import tools.descartes.autoscaling.dummy.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.dummy.learning.smile.SMILEModel;
import tools.descartes.autoscaling.dummy.learning.smile.SuperSMILERegressor;
import tools.descartes.autoscaling.dummy.metrics.Metric;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategy;
import tools.descartes.autoscaling.dummy.training.ModelTrainer;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataImporter;

/**
 * Class responsible for training a model.
 *
 *
 */
public class SMILEModelTrainer implements ModelTrainer {

	private final Logger logger = LoggerFactory.getLogger(SMILEModelTrainer.class);
	private final IRewardFunction reward;
	private final String app;
	private final List<KPIProperties> kpis;
	private final String instances;
	private final MissingValueStrategy missingValueStrategy;

	/**
	 * Creates a new instance of a Trainer. No data checking is done on
	 * initialization. Instead, this is done lazily when calling retrain.
	 * 
	 * @param app       The app this trainer is for. Is also utilized for looking
	 *                  for training data.
	 * @param reward    The reward function to use to analyze states.
	 * @param kpis      The kpis that are relevant for calculating the reward.
	 * @param instances The name of the instance column that are relevant for
	 *                  calculating the reward.
	 */
	public SMILEModelTrainer(String app, IRewardFunction reward, List<KPIProperties> kpis, String instances,
			MissingValueStrategy missingValueStrategy) {
		super();
		this.reward = reward;
		this.app = app;
		this.kpis = kpis;
		this.instances = instances;
		this.missingValueStrategy = missingValueStrategy;
	}

	@Override
	public IRLModel trainModel(DataFrame data) {
		if (data == null) {
			logger.info("No training data available for app " + app);
			return null;
		}
		// clean data
		// data = data.omitNullRows(); (Is done in cleanData metohd)
		try {
			data = cleanData(data);
		} catch (Exception e) {
			logger.error("Could not clean DataFrame, nested exception is: ", e);
			return null;
		}
		// add reward
		try {
			data = calculateReward(data);
		} catch (Exception e) {
			logger.error("Could not calculate reward, nested exception is: ", e);
			return null;
		}

		StructType dataschema = data.schema();
		logger.trace(data.toString());
		logger.info("Training a new model for app \"" + app + "\" using " + data.nrows() + " samples, with "
				+ (data.ncols() - 1) + " features each.");

		// training
		Formula f = Formula.lhs("Reward");
		try {
			Regression<Tuple> trainedModel = new SuperSMILERegressor(f, data);
			SMILEModel model = new SMILEModel(app, trainedModel, dataschema, missingValueStrategy);
			logger.trace(model.getModel().toString());
			return model;
		} catch (IllegalStateException ise) {
			logger.info("No model type for app " + app + " could be trained");
			return null;
		}
	}

	@Override
	public IRLModel trainModel(TrainingDataImporter dataImporter) {
		DataFrame data = dataImporter.getTrainingDataForApp(app);
		return trainModel(data);
	}

	public DataFrame cleanData(DataFrame data) {
		data = data.omitNullRows();
		data = removeConstantCols(data);
		// drop the time stamp
		data = data.drop("time");
		return data;
	}

	private DataFrame removeConstantCols(DataFrame data) {
		ArrayList<String> toDrop = new ArrayList<>();
		for (int i = 0; i < data.ncols(); i++) {
			BaseVector<?, ?, ?> col = data.column(i);

			// we currently only consider doubles
			double first = col.getDouble(0);
			boolean different = false;
			for (int j = 1; j < col.size(); j++) {
				// if only one is not the same as the first entry -> not constant
				if (first != col.getDouble(j)) {
					different = true;
					break;
				}
			}
			if (!different) {
				toDrop.add(col.name());
			}
		}

		for (String string : toDrop) {
			data = data.drop(string);
			logger.debug("Dropped column \"" + string + "\" from trainingdata of app \"" + app
					+ "\" because its value was constant.");
		}
		return data;

	}

	public DataFrame calculateReward(DataFrame data) {
		double[][] reward = new double[data.nrows()][1];
		for (int i = 0; i < reward.length - 1; i++) {
			List<Metric> kpiVals = new ArrayList<>();
			for (KPIProperties kpi : kpis) {
				double kpiVal = data.getDouble(i + 1, kpi.getName());
				kpiVals.add(Metric.of(kpi.getName(), kpiVal, app));
			}
			double instanceVal = data.getDouble(i + 1, this.instances);
			reward[i][0] = this.reward.getReward(instanceVal, kpiVals, kpis);
		}
		reward[reward.length - 1][0] = 0;
		data = data.merge(DataFrame.of(reward, "Reward"));
		return data;
	}

}
