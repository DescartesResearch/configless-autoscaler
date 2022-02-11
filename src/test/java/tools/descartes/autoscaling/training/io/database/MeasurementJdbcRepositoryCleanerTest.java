package tools.descartes.autoscaling.training.io.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.descartes.autoscaling.config.TrainingProperties;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MeasurementJdbcRepositoryCleanerTest {

    private MeasurementJdbcRepositoryCleaner sut;

    private TrainingProperties trainingProperties;

    @Mock
    MeasurementJdbcRepository measurementJdbcRepositoryMock;

    @Captor
    ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor;

    @BeforeEach
    void setup() {
        trainingProperties = new TrainingProperties();
        trainingProperties.setTrainingDatabaseNumberDaysToKeep(10);
        sut = new MeasurementJdbcRepositoryCleaner(measurementJdbcRepositoryMock, trainingProperties);
    }

    @Test
    public void cleans() {
        // when
        sut.removeOutdatedMeasurements();
        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1))
                .deleteAllMeasurementsBefore(localDateTimeArgumentCaptor.capture());
        // assert that we keep at least 10 days
        assertThat(localDateTimeArgumentCaptor.getValue()).isBefore(LocalDateTime.now().minusDays(9));
        assertThat(localDateTimeArgumentCaptor.getValue()).isAfter(LocalDateTime.now().minusDays(11));
    }

}