package tools.descartes.autoscaling.dummy.training.io;

import smile.data.DataFrame;

@FunctionalInterface
public interface TrainingDataImporter {
    /**
     * @param app Name of app for training
     * @return training data for this app
     */
    DataFrame getTrainingDataForApp(String app);
}
