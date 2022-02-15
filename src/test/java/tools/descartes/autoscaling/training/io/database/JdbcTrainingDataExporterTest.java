package tools.descartes.autoscaling.training.io.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.descartes.autoscaling.config.TrainingProperties;
import tools.descartes.autoscaling.metrics.Metric;
import tools.descartes.autoscaling.metrics.SimpleMetric;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JdbcTrainingDataExporterTest {

    private static final ObjectMapper TEST_OBJECT_MAPPER = new ObjectMapper();
    private static final long TIME_NOW = System.currentTimeMillis();

    private TrainingProperties trainingProperties;
    private JdbcTrainingDataExporter sut;

    @Mock
    MeasurementJdbcRepository measurementJdbcRepositoryMock;

    @Captor
    ArgumentCaptor<List<MeasurementJdbcEntity>> jdbcEntityCaptor;

    @BeforeEach
    void setup() {
        trainingProperties = new TrainingProperties();
        trainingProperties.setExporter(new TrainingProperties.ExportProperties());
        trainingProperties.getExporter().setMetricStorageExclusionRegex(null);
        sut = new JdbcTrainingDataExporter(TEST_OBJECT_MAPPER, measurementJdbcRepositoryMock, trainingProperties);
    }

    @Test
    public void convertsSystemMillisToLocalDateTime() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        long systemMillis = System.currentTimeMillis();

        LocalDateTime actual = JdbcTrainingDataExporter.systemMillisToLocalDateTime(systemMillis);

        assertThat(actual).isAfterOrEqualTo(now);
        assertThat(actual).isBefore(now.plusSeconds(1));
    }

    @Test
    public void convertsLocalDateTimeToSystemMillis() {
        long systemMillis = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        long actual = JdbcTrainingDataExporter.localDateTimeToSystemTimeMillis(now);

        assertThat(actual).isGreaterThanOrEqualTo(systemMillis);
        assertThat(actual).isCloseTo(systemMillis, Offset.offset(50L));
    }

    @Test
    public void givenMetricsAndNoExclusionPattern_whenCommittingEmptyActions_ActionsAreCommitted() {
        // given, when
        sut.commitActions(TIME_NOW, createMetrics(), Collections.emptyMap());

        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1)).insert(jdbcEntityCaptor.capture());
        List<MeasurementJdbcEntity> entities = jdbcEntityCaptor.getValue();
        assertThat(entities).hasSize(3);
        assertThat(entities).map(MeasurementJdbcEntity::getMeasurementTime)
                .containsOnly(JdbcTrainingDataExporter.systemMillisToLocalDateTime(TIME_NOW));
        assertThat(entities).map(MeasurementJdbcEntity::getApp).containsExactlyInAnyOrder("app0", "app1", "app2");
        assertThat(entities).map(MeasurementJdbcEntity::getActions).containsExactlyInAnyOrder(0, 0, 0);
        assertThat(entities).map(MeasurementJdbcEntity::getMeasurementJson)
                .containsExactlyInAnyOrder(
                        "[{\"name\":\"metric0\",\"value\":1.0}]",
                        "[{\"name\":\"metric2\",\"value\":3.0}]",
                        "[{\"name\":\"metric1_pattern\",\"value\":2.0}]"
                );
    }

    @Test
    public void givenMetricsAndNoExclusionPattern_whenCommittingActions_ActionsAreCommitted() {
        // given, when
        sut.commitActions(TIME_NOW, createMetrics(), createActions());

        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1)).insert(jdbcEntityCaptor.capture());
        List<MeasurementJdbcEntity> entities = jdbcEntityCaptor.getValue();
        assertThat(entities).hasSize(3);
        assertThat(entities).map(MeasurementJdbcEntity::getActions).doesNotContainNull();
        assertThat(entities).map(MeasurementJdbcEntity::getActions).containsExactlyInAnyOrder(0, 0, -1);
        assertThat(entities).map(MeasurementJdbcEntity::getMeasurementJson)
                .containsExactlyInAnyOrder(
                        "[{\"name\":\"metric0\",\"value\":1.0}]",
                        "[{\"name\":\"metric2\",\"value\":3.0}]",
                        "[{\"name\":\"metric1_pattern\",\"value\":2.0}]"
                );
    }

    @Test
    public void givenMetricsAndNoExporterProperties_whenCommittingActions_ActionsAreCommitted() {
        // given
        trainingProperties.setExporter(null);
        // when
        sut.commitActions(TIME_NOW, createMetrics(), Collections.emptyMap());

        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1)).insert(jdbcEntityCaptor.capture());
        assertThat(jdbcEntityCaptor.getValue()).hasSize(3);
    }

    @Test
    public void givenErrorInDb_whenCommittingActions_errorIsCaughtAndSkipped() {
        // given
        Mockito.when(measurementJdbcRepositoryMock.insert(Mockito.any())).thenThrow(new UncheckedIOException(new IOException()));
        // when
        sut.commitActions(TIME_NOW, createMetrics(), Collections.emptyMap());
        // then, no exceptions
    }

    @Test
    public void givenMetricsAndExclusionPattern_whenCommittingActions_ActionsAreCommittedForNonExcluded() {
        // given
        trainingProperties.getExporter().setMetricStorageExclusionRegex(".*_pattern.*");
        sut = new JdbcTrainingDataExporter(TEST_OBJECT_MAPPER, measurementJdbcRepositoryMock, trainingProperties);

        // when
        sut.commitActions(TIME_NOW, createMetrics(), createActions());

        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1)).insert(jdbcEntityCaptor.capture());
        List<MeasurementJdbcEntity> entities = jdbcEntityCaptor.getValue();
        assertThat(entities).hasSize(3);
        assertThat(entities).map(MeasurementJdbcEntity::getMeasurementJson)
                .containsExactlyInAnyOrder(
                        "[{\"name\":\"metric0\",\"value\":1.0}]",
                        "[{\"name\":\"metric2\",\"value\":3.0}]",
                        "[]"
                );
    }

    private static List<Metric> createMetrics() {
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new SimpleMetric("metric0", "app0", 1.0));
        metrics.add(new SimpleMetric("metric1_pattern", "app1", 2.0));
        metrics.add(new SimpleMetric("metric2", "app2", 3.0));
        return metrics;
    }

    private static Map<String, Integer> createActions() {
        Map<String, Integer> actions = new HashMap<>();
        actions.put("app0", -1);
        actions.put("app1", 0);
        actions.put("app2", 0);
        return actions;
    }

}