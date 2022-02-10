package tools.descartes.autoscaling.dummy.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.TrainingProperties;
import tools.descartes.autoscaling.dummy.knowledge.InstanceManager;
import tools.descartes.autoscaling.dummy.knowledge.ModelRegistry;
import tools.descartes.autoscaling.dummy.learning.IRLModel;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataImporter;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataImporterWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The core class of the training package
 */
@Component
public class Trainer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Trainer.class);

    private final AtomicBoolean isRunning;
    private final TrainingDataImporter trainingDataImporter;
    private final ModelTrainerFactory trainerFactory;
    private final InstanceManager instanceManager;
    private final ModelRegistry modelRegistry;
    private final TrainingProperties trainingProperties;

    public Trainer(TrainingDataImporterWrapper trainingDataImporterWrapper, ModelTrainerFactory trainerFactory,
                   InstanceManager instanceManager, ModelRegistry modelRegistry, TrainingProperties trainingProperties) {
        isRunning = new AtomicBoolean(false);
        this.trainingDataImporter = trainingDataImporterWrapper.getDataImporter();
        this.trainerFactory = trainerFactory;
        this.instanceManager = instanceManager;
        this.modelRegistry = modelRegistry;
        this.trainingProperties = trainingProperties;
    }

    @Override
    public void run() {
        isRunning.set(true);
        // if training interval is negative no models are trained and used
        if (trainingProperties.getInterval() < 0) {
            logger.info("Training Interval set to negative number, models wont be used and trained");
            this.isRunning.set(false);
            return;
        }
        Map<String, ModelTrainer> trainerMap = new HashMap<>();
        while (this.isRunning.get()) {
            Set<String> apps = instanceManager.getApps();
            if (apps != null && !apps.isEmpty()) {
                for (String app : apps) {
                    ModelTrainer trainer;
                    if (trainerMap.containsKey(app)) {
                        trainer = trainerMap.get(app);
                    } else {
                        trainer = trainerFactory.getModelTrainer(app);
                        if (trainer != null) trainerMap.put(app, trainer);
                        else continue;
                    }
                    logger.debug("Training model for app " + app);
                    IRLModel model = trainer.trainModel(trainingDataImporter);
                    if (model != null) {
                        logger.debug("Training model for app " + app + " finished");
                        modelRegistry.setModelForApp(app, model);
                    } else {
                        logger.info("Training for app " + app + " was not successful");
                    }
                }
            } else {
                logger.info("InstanceManager seems to be not ready yet, no training possible at the moment, waiting 30s...");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ie) {
                    logger.error("InterruptedException while scaling pause");
                }
                continue;
            }
            int timeoutSeconds = trainingProperties.getInterval();
            // If training interval is zero, only initial training is done, no retraining
            if (trainingProperties.getInterval() == 0) {
                break;
            }
            try {
                Thread.sleep(timeoutSeconds * 1000L);
            } catch (InterruptedException ie) {
                logger.error("InterruptedException while scaling pause");
            }
        }
        this.isRunning.set(false);
    }

    public void exit() {
        isRunning.set(false);
    }
}
