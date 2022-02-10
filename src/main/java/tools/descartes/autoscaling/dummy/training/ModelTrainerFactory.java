package tools.descartes.autoscaling.dummy.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.ScalingProperties;
import tools.descartes.autoscaling.dummy.learning.reward.IRewardFunction;
import tools.descartes.autoscaling.dummy.learning.reward.RewardFunctionWrapper;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategy;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategyWrapper;
import tools.descartes.autoscaling.dummy.training.smile.SMILEModelTrainer;

import java.util.List;
import java.util.Map;

import static tools.descartes.autoscaling.dummy.config.ScalingProperties.KPIProperties;

/**
 * A factory class which creates new instances of ModelTrainer for different apps
 */
@Component
public class ModelTrainerFactory {

    private static final Logger logger = LoggerFactory.getLogger(ModelTrainerFactory.class);

    private final IRewardFunction reward;
    private final String instances;
    private final MissingValueStrategy missingValueStrategy;
    private final Map<String, List<KPIProperties>> kpis;

    public ModelTrainerFactory(RewardFunctionWrapper rewardFunctionWrapper, ScalingProperties scalingProperties,
                               MissingValueStrategyWrapper missingValueStrategyWrapper) {
        this.reward = rewardFunctionWrapper.getRewardFunction();
        this.instances = scalingProperties.getInstances();
        this.missingValueStrategy = missingValueStrategyWrapper.getDefaultStrategy();
        this.kpis = scalingProperties.getKpis();
    }

    public ModelTrainer getModelTrainer(String app) {
        List<KPIProperties> kpisForApp = kpis.get(app);
        if (kpisForApp == null || kpisForApp.isEmpty()) {
            logger.info("No KPIs given for app " + app + ", model cannot be trained");
            return null;
        }
        return new SMILEModelTrainer(app, reward, kpisForApp, instances, missingValueStrategy);
    }
}
