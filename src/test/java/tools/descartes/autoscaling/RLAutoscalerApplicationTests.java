package tools.descartes.autoscaling;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.regression.RandomForest;
import smile.validation.CrossValidation;
import tools.descartes.autoscaling.config.ScalingProperties.KPIProperties;
import tools.descartes.autoscaling.learning.IRLModel;
import tools.descartes.autoscaling.learning.actionspace.ActionSpace;
import tools.descartes.autoscaling.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.learning.reward.ImprovedRewardFunction;
import tools.descartes.autoscaling.learning.smile.SMILEModel;
import tools.descartes.autoscaling.learning.smile.SuperSMILERegressor;
import tools.descartes.autoscaling.metrics.Metric;
import tools.descartes.autoscaling.metrics.SimpleMetric;
import tools.descartes.autoscaling.metrics.missing.ConstantStrategy;
import tools.descartes.autoscaling.training.io.csv.CSVTrainingDataImporter;
import tools.descartes.autoscaling.training.smile.SMILEModelTrainer;

@SpringBootTest
class RLAutoscalerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testSMILEPredictions() {
		// just tests, if the model inference works without throwing exceptions...
		IRewardFunction reward = new ImprovedRewardFunction();
		KPIProperties kpi = new KPIProperties();
		kpi.setName("metric0");
		kpi.setSlo(0.8);
		List<KPIProperties> kpis = new ArrayList<>();
		kpis.add(kpi);
		String instances = "metric1";
		SMILEModelTrainer t = new SMILEModelTrainer("app1", reward, kpis, instances,
				new ConstantStrategy(-1));
		ArrayList<String> files = new ArrayList<>();
		files.add("resources/old_trainingdata");
		CSVTrainingDataImporter csvTrainingDataImporter = new CSVTrainingDataImporter(files);
		DataFrame f = csvTrainingDataImporter.getTrainingDataForApp("app1");
		IRLModel m = t.trainModel(f);
		double result = ((SMILEModel) m).getModel().predict(f.get(200));
		System.out.println("Predicted result :" + result);
	}

	
	@Test
	void testModelInferences() {
		
		KPIProperties kpi = new KPIProperties();
		kpi.setName("metric0");
		kpi.setSlo(0.8);
		List<KPIProperties> kpis_gateway = new ArrayList<>();
		kpis_gateway.add(kpi);
		testOneServiceInference("app1", kpis_gateway);
	}
	
	
	private void testOneServiceInference(String service, List<KPIProperties> kpis) {
		// just tests, if the model inference works without throwing exceptions...
		IRewardFunction reward = new ImprovedRewardFunction();
		String instances = "metric1";
		ArrayList<String> files = new ArrayList<>();
		files.add("resources/old_trainingdata");
		CSVTrainingDataImporter csvTrainingDataImporter = new CSVTrainingDataImporter(files);
		DataFrame data = csvTrainingDataImporter.getTrainingDataForApp(service);
		SMILEModelTrainer t = new SMILEModelTrainer(service, reward, kpis, instances,
				new ConstantStrategy(-1));
		data = data.omitNullRows();
		data = t.calculateReward(data);
		data = t.cleanData(data);
		Formula f = Formula.lhs("Reward");
		IRLModel m = new SMILEModel(service, new SuperSMILERegressor(f, data), data.schema(),
				new ConstantStrategy(-1));

		List<Metric> state = turnStateToMetric(data.get(212), service);
		System.out.println(state);
		System.out.println(turnStateToMetric(data.get(213), service));

		System.out.println(m.predictBestAction(state, ActionSpace.getDefaultActionSpace()));
	}

	private List<Metric> turnStateToMetric(Tuple t, String app) {
		ArrayList<Metric> list = new ArrayList<>();
		for (int i = 0; i < t.schema().length(); i++) {
			if (!t.schema().fieldName(i).equals("action")) {
				Metric e = new SimpleMetric(t.schema().fieldName(i), app, t.getDouble(i));
				list.add(e);
			}
		}
		return list;
	}

	@Test
	void evaluateSMILEModelsInferenceGateway() {
		// just tests, if the model inference works without throwing exceptions...
		IRewardFunction reward = new ImprovedRewardFunction();
		KPIProperties kpi = new KPIProperties();
		kpi.setName("metric0");
		kpi.setSlo(0.8);
		List<KPIProperties> kpis = new ArrayList<>();
		kpis.add(kpi);
		String instances = "metric1";
		ArrayList<String> files = new ArrayList<>();
		files.add("resources/old_trainingdata");
		CSVTrainingDataImporter csvTrainingDataImporter = new CSVTrainingDataImporter(files);
		DataFrame data = csvTrainingDataImporter.getTrainingDataForApp("app1");
		SMILEModelTrainer t = new SMILEModelTrainer("app1", reward, kpis, instances,
				new ConstantStrategy(-1));
		data = data.omitNullRows();
		data = t.calculateReward(data);
		data = t.cleanData(data);
		System.out.println(data);

		System.out.println(data.summary());

		// training
		Formula f = Formula.lhs("Reward");
//		System.out.println("LinReg:");
//		System.out.println(CrossValidation.regression(5, f, data, OLS::fit));
//		System.out.println("Lasso Regression:");
//		System.out.println(CrossValidation.regression(5, f, data, LASSO::fit));
//		System.out.println("Ridge regression:");
//		System.out.println(CrossValidation.regression(5, f, data, RidgeRegression::fit));
//		System.out.println("Regression tree:");
//		System.out.println(CrossValidation.regression(5, f, data, RegressionTree::fit));

		System.out.println("Random Forest:");
		System.out.println(CrossValidation.regression(5, f, data, RandomForest::fit));

		RandomForest modle = RandomForest.fit(f, data);

		for (int i = 0; i < modle.importance().length; i++) {
			System.out.println(modle.importance()[i]);
		}
	}

}