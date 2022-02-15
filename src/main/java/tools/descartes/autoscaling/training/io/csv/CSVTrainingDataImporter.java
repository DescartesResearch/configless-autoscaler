package tools.descartes.autoscaling.training.io.csv;

import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;
import smile.io.Read;
import tools.descartes.autoscaling.training.io.TrainingDataImporter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of TrainingDataImporter which imports training data from a CSV file
 */
public class CSVTrainingDataImporter implements TrainingDataImporter {

    private static final Logger logger = LoggerFactory.getLogger(CSVTrainingDataImporter.class);

    private final List<String> paths;

    public CSVTrainingDataImporter(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public DataFrame getTrainingDataForApp(String app) {
        List<String> files = new ArrayList<>();
        for (String path : paths) {
            File f = new File( path + "/" + app + "_actions.csv");
            if (!f.exists()) {
                logger.warn("Looking for training file \"" + f.getAbsolutePath() + "\" but could not find it.");
            } else {
                files.add(f.getAbsolutePath());
            }
        }

        ArrayList<DataFrame> frames = new ArrayList<>();
        for (String file : files) {
            try {
                frames.add(loadDataFromFile(file));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (frames.size() == 0) {
            logger.warn("There has been an error, as no file could be successfully loaded for training app: " + app);
            return null;
        }
        // merge frames for returning
        DataFrame start = frames.get(0);
        frames.remove(0);
        if (frames.size() > 0) {
            return start.union(frames.toArray(new DataFrame[0]));
        } else {
            return start;
        }
    }

    private DataFrame loadDataFromFile(String path) throws IOException, URISyntaxException {
        CSVFormat format = CSVFormat.DEFAULT.withFirstRecordAsHeader();
        return Read.csv(path, format);
    }
}
