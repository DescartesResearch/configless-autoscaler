package tools.descartes.autoscaling.dummy.training.io.database;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import tools.descartes.autoscaling.dummy.config.TrainingProperties;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategy;
import tools.descartes.autoscaling.dummy.metrics.missing.MissingValueStrategyWrapper;
import tools.descartes.autoscaling.dummy.training.io.TrainingDataImporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * An implementation of TrainingDataImporter which imports training data from a database using JDBC
 */
@Component
public class JdbcTrainingDataImporter implements TrainingDataImporter {

    private static final Logger logger = LoggerFactory.getLogger(JdbcTrainingDataImporter.class);
    private static final String TIME_DATA_FIELD_NAME = "time";
    private static final String ACTION_DATA_FIELD_NAME = "action";


    private final TrainingProperties trainingProperties;
    private final MeasurementJdbcRepository measurementJdbcRepository;
    private final MissingValueStrategy defaultMissingValueStrategy;
    private final ObjectMapper objectMapper;
    private final JavaType jsonMetricListType;

    public JdbcTrainingDataImporter(
            TrainingProperties trainingProperties, MeasurementJdbcRepository measurementJdbcRepository,
            MissingValueStrategyWrapper missingValueStrategyWrapper,
            ObjectMapper objectMapper
    ) {
        this.trainingProperties = trainingProperties;
        this.measurementJdbcRepository = measurementJdbcRepository;
        this.defaultMissingValueStrategy = missingValueStrategyWrapper.getDefaultStrategy();
        this.objectMapper = objectMapper;
        this.jsonMetricListType = objectMapper.getTypeFactory().constructCollectionType(List.class, JsonMetric.class);
    }

    @Override
    public DataFrame getTrainingDataForApp(String app) {
        List<MeasurementJdbcEntity> entites = measurementJdbcRepository
                .measurementsSince(app, LocalDateTime.now().minusDays(trainingProperties.getTrainingLookbackDays()));
        if (entites.isEmpty()) {
            logger.warn("No measurement entities found for training. Returning null.");
            return null;
        }
        List<String> metricHeaders = inferMetricHeaders(entites);
        logger.info("Importing training data for app={} with #rows={} and #colums={}",
                app, entites.size(), metricHeaders.size() + 2);
        StructType schema = createSchema(metricHeaders);
        Iterator<MeasurementJdbcEntity> entityIt = entites.iterator();
        DataFrame dataFrame = toDataFrame(schema, valuesAsRow(metricHeaders, entityIt.next()));
        while (entityIt.hasNext()) {
            dataFrame = dataFrame.union(toDataFrame(schema, valuesAsRow(metricHeaders, entityIt.next())));
        }
        return dataFrame;
    }

    private List<String> inferMetricHeaders(List<MeasurementJdbcEntity> entites) {
        Set<String> metricHeaders = new HashSet<>();
        for (MeasurementJdbcEntity entity : entites) {
            List<JsonMetric> metrics = getMetricsFromJsonClob(entity);
            metrics.forEach(m -> metricHeaders.add(m.getName()));
        }
        return new ArrayList<>(metricHeaders);
    }

    private Object[] valuesAsRow(List<String> metricHeaders, MeasurementJdbcEntity entity) {
        Object[] values = new Object[metricHeaders.size() + 2];
        values[0] = JdbcTrainingDataExporter.localDateTimeToSystemTimeMillis(entity.getMeasurementTime());
        values[1] = entity.getActions();
        int i = 2;
        for (double value : getMetricValuesInHeaderOrder(metricHeaders, entity)) {
            values[i] = value;
            i++;
        }
        return values;
    }

    private List<Double> getMetricValuesInHeaderOrder(List<String> metricHeaders, MeasurementJdbcEntity entity) {
        Map<String, JsonMetric> metricsByName = new HashMap<>();
        List<JsonMetric> metrics = getMetricsFromJsonClob(entity);
        metrics.forEach(m -> metricsByName.put(m.getName(), m));
        List<Double> values = new ArrayList<>(metricHeaders.size());
        for (String header : metricHeaders) {
            JsonMetric metric = metricsByName.get(header);
            if (metric != null) {
                values.add(metric.getValue());
            } else {
                values.add(defaultMissingValueStrategy.getDefaultValue());
            }
        }
        return values;
    }

    private List<JsonMetric> getMetricsFromJsonClob(MeasurementJdbcEntity entity) {
        try {
            return objectMapper.readValue(entity.getMeasurementJson(), jsonMetricListType);
        } catch (IOException e) {
            logger.error("Error parsing metrics from stored json: ", e);
            throw new UncheckedIOException(e);
        }
    }

    private static DataFrame toDataFrame(StructType schema, Object[] valueRow) {
        return DataFrame.of(Collections.singletonList(Tuple.of(valueRow, schema)), schema);
    }

    private static StructType createSchema(List<String> metricHeaders) {
        List<StructField> fieldTypes = new ArrayList<>(metricHeaders.size() + 2);
        fieldTypes.add(new StructField(TIME_DATA_FIELD_NAME, DataTypes.LongType));
        fieldTypes.add(new StructField(ACTION_DATA_FIELD_NAME, DataTypes.IntegerType));
        for (String metricHeader: metricHeaders) {
            fieldTypes.add(new StructField(metricHeader, DataTypes.DoubleType));
        }
        return new StructType(fieldTypes);
    }
}
