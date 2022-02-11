package tools.descartes.autoscaling.learning.smile;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.regression.DataFrameRegression;
import smile.regression.RandomForest;
import smile.regression.Regression;
import smile.regression.RegressionTree;
import smile.validation.CrossValidation;

/**
 * A class which compares alternative regression models and uses the best model for prediction
 * We use the mean absolute deviation as an error metric and RegressionTree and Random Forest
 * as possible models (Note: Not all models might work for all training data, especially when data
 * contain constant columns)
 */
public class SuperSMILERegressor implements Regression<Tuple> {

	private static final Logger logger = LoggerFactory.getLogger(SuperSMILERegressor.class);

	private final DataFrameRegression predictor;

	private final Map<String, BiFunction<Formula, DataFrame, DataFrameRegression>> regressors;

	public SuperSMILERegressor(Formula f, DataFrame data) {

		regressors = new HashMap<>();
		try {
			regressors.put("Random Forest", RandomForest::fit);
		} catch (Exception e) {
			logger.info("Could not train RandomForest, nested exception is: ", e);
		}
		try {
			regressors.put("RegressionTree", RegressionTree::fit);
		} catch (Exception e) {
			logger.info("Could not train RegressionTree, nested exception is: ", e);
		}

		this.predictor = chooseBestModel(f, data);
	}

	private DataFrameRegression chooseBestModel(Formula f, DataFrame data) {
		if (regressors.isEmpty()) {
			throw new IllegalStateException("No models are defined for training.");
		}
		double currMin = Double.MAX_VALUE;
		Entry<String, BiFunction<Formula, DataFrame, DataFrameRegression>> currBest = null;
		for (Entry<String, BiFunction<Formula, DataFrame, DataFrameRegression>> entry : regressors.entrySet()) {
			BiFunction<Formula, DataFrame, DataFrameRegression> func = entry.getValue();
			double error;
			try {
				error = CrossValidation.regression(5, f, data, func).avg.mad;
				logger.debug(
						"Training of \"" + entry.getKey() + "\" achieved a cross-validation error of: " + error + ".");
			} catch (IllegalArgumentException e) {
				logger.info("Could not execute Cross-Validation for \"" + entry.getKey() + "\" because at least one fold failed, nested exception is: ", e);
				if (currBest == null) currBest = entry;
				continue;
			}
			if (error < currMin) {
				currMin = error;
				currBest = entry;
			}
		}
		if (currMin == Double.MAX_VALUE) {
			logger.info("All models did not succeed 5-fold cross validation, possibly there are only few training data");
			logger.info("Trying out model " + currBest.getKey());
		} else {
			logger.info("Choose the \"" + currBest.getKey() + "\" as best model with a cross-validation error of : "
					+ currMin + " during training.");
		}
		return currBest.getValue().apply(f, data);
	}

	@Override
	public double predict(Tuple x) {
		return predictor.predict(x);
	}

}
