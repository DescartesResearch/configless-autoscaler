package tools.descartes.autoscaling.training;

import smile.data.DataFrame;
import tools.descartes.autoscaling.learning.IRLModel;
import tools.descartes.autoscaling.training.io.TrainingDataImporter;

public interface ModelTrainer {

    /**
     * Train model directly on a DataFrame
     */
    IRLModel trainModel(DataFrame data);

    /**
     * Train model by importing data from a TrainingDataImporter
     */
    IRLModel trainModel(TrainingDataImporter dataImporter);
}
