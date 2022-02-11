package tools.descartes.autoscaling.training.io.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smile.data.DataFrame;
import tools.descartes.autoscaling.config.TrainingProperties;
import tools.descartes.autoscaling.metrics.missing.MissingValueStrategyWrapper;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcTrainingDataImporterTest {

    private static final String APP = "app1";
    private static final LocalDateTime NOW = LocalDateTime.of(2020, 1, 1, 1, 30);

    private static final ObjectMapper TEST_OBJECT_MAPPER = new ObjectMapper();

    private TrainingProperties trainingProperties;
    private MissingValueStrategyWrapper missingValueStrategyWrapper;

    @Mock
    MeasurementJdbcRepository measurementJdbcRepositoryMock;

    private JdbcTrainingDataImporter sut;

    @BeforeEach
    void setup() {
        trainingProperties = new TrainingProperties();
        trainingProperties.setTrainingLookbackDays(30);
        missingValueStrategyWrapper = new MissingValueStrategyWrapper();
        sut = new JdbcTrainingDataImporter(
                trainingProperties, measurementJdbcRepositoryMock, missingValueStrategyWrapper, TEST_OBJECT_MAPPER
        );
    }

    @Test
    public void givenNoMeasurements_whenGettingTrainingData_thenEmptyDataFrame() {
        when(measurementJdbcRepositoryMock.measurementsSince(eq(APP), any())).thenReturn(Collections.emptyList());

        DataFrame actual = sut.getTrainingDataForApp(APP);

        assertThat(actual).isNull();
    }

    @Test
    public void givenInvalidJsonInEntity_whenGettingTrainingData_thenExceptionIsThrown() {
        when(measurementJdbcRepositoryMock.measurementsSince(eq(APP), any())).thenReturn(Collections.singletonList(
                new MeasurementJdbcEntity(NOW, APP,
                        "[{\"value\":\"notADouble\"}]", null)
        ));

        assertThrows(UncheckedIOException.class, () -> sut.getTrainingDataForApp(APP));
    }

    @Test
    public void givenMeasurements_whenGettingTrainingData_thenDataFrame() {
        when(measurementJdbcRepositoryMock.measurementsSince(eq(APP), any())).thenReturn(createEntityList());

        DataFrame actual = sut.getTrainingDataForApp(APP);

        System.out.println(actual);

        assertThat(actual.nrows()).isEqualTo(5);
        assertThat(actual.ncols()).isEqualTo(6);
        assertThat(actual.names()).containsExactly("time", "action", "metric0", "metric1", "metric2", "metric3");
        assertThat(actual.column(0).toStringArray()).containsExactly("1577838300000","1577838360000",
                "1577838420000","1577838480000","1577838540000");
        assertThat(actual.column(1).toIntArray()).containsExactly(0, 0, -1, 1, 0);
        assertThat(actual.column(2).toDoubleArray()).containsOnly(new double[]{1.0}, Offset.offset(0.0001));
        assertThat(actual.column(3).toDoubleArray())
                .containsExactly(new double[]{2.0, -1.0, -1.0, 2.0, 2.0}, Offset.offset(0.0001));
        assertThat(actual.column(5).toDoubleArray())
                .containsExactly(new double[]{-1.0, -1.0, 4.0, -1.0, -1.0}, Offset.offset(0.0001));
    }

    private List<MeasurementJdbcEntity> createEntityList() {
        List<MeasurementJdbcEntity> entites = new ArrayList<>();
        entites.add(new MeasurementJdbcEntity(NOW.minusMinutes(5), APP,
                "[{\"name\":\"metric0\",\"value\":1.0},{\"name\":\"metric1\",\"value\":2.0}]", 0));
        entites.add(new MeasurementJdbcEntity(NOW.minusMinutes(4), APP,
                "[{\"name\":\"metric0\",\"value\":1.0},{\"name\":\"metric2\",\"value\":3.0}]", 0));
        entites.add(new MeasurementJdbcEntity(NOW.minusMinutes(3), APP,
                "[{\"name\":\"metric0\",\"value\":1.0},{\"name\":\"metric3\",\"value\":4.0}]", -1));
        entites.add(new MeasurementJdbcEntity(NOW.minusMinutes(2), APP,
                "[{\"name\":\"metric0\",\"value\":1.0},{\"name\":\"metric1\",\"value\":2.0}]", 1));
        entites.add(new MeasurementJdbcEntity(NOW.minusMinutes(1), APP,
                "[{\"name\":\"metric0\",\"value\":1.0},{\"name\":\"metric1\",\"value\":2.0}]", 0));
        return entites;
    }

}