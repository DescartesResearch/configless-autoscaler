package tools.descartes.autoscaling.dummy.training.io.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MeasurementJdbcRepositoryCleanerTest {

    private MeasurementJdbcRepositoryCleaner sut;

    @Mock
    MeasurementJdbcRepository measurementJdbcRepositoryMock;

    @Captor
    ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor;

    @BeforeEach
    void setup() {
        sut = new MeasurementJdbcRepositoryCleaner(measurementJdbcRepositoryMock);
    }

    @Test
    public void cleans() {
        // when
        sut.removeOutdatedMeasurements();
        // then
        Mockito.verify(measurementJdbcRepositoryMock, Mockito.times(1))
                .deleteAllMeasurementsBefore(localDateTimeArgumentCaptor.capture());
        // assert that we keep at least several days, but not 3 years
        assertThat(localDateTimeArgumentCaptor.getValue()).isBefore(LocalDateTime.now().minusDays(20));
        assertThat(localDateTimeArgumentCaptor.getValue()).isAfter(LocalDateTime.now().minusYears(3));
    }

}