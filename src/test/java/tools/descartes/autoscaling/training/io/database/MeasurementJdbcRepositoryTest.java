package tools.descartes.autoscaling.training.io.database;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest()
class MeasurementJdbcRepositoryTest {

    private static final LocalDateTime TIME_NOW = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    private static final String APP = "app0";

    @Autowired
    private MeasurementJdbcRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        jdbcTemplate.update("DELETE from measurements");
    }

    @Test
    public void givenNoMeasurement_whenInserting_thenNothingHappens() {
        // given, when
        int actualUpdated = sut.insert(null);

        // then
        int actualCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM measurements", Integer.class);
        assertEquals(0, actualCount);
        assertEquals(actualCount, actualUpdated);
    }


    @Test
    public void givenMeasurement_whenInsertingMeasurement_thenMeasurementIsInserted() {
        // given, when
        int actualUpdated = sut.insert(
                Collections.singletonList(new MeasurementJdbcEntity(TIME_NOW, APP, "asdf", 0))
        );

        // then
        int actualCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM measurements", Integer.class);
        assertEquals(1, actualCount);
        assertEquals(actualCount, actualUpdated);
    }


    @Test
    public void givenMeasurements_whenGettingMeasurementsSince_thenMeasurementsSinceAreReturnedWithCorrectApp() {
        List<MeasurementJdbcEntity> givenMeasurements = Arrays.asList(
                new MeasurementJdbcEntity(TIME_NOW.minusNanos(1000), APP, "asdf", 0),
                new MeasurementJdbcEntity(TIME_NOW.plusNanos(1000), APP, "asdf", 0),
                new MeasurementJdbcEntity(TIME_NOW, APP, "asdf", -1),
                new MeasurementJdbcEntity(TIME_NOW.plusNanos(1000), "wrongApp", "asdf", 0)
        );
        sut.insert(givenMeasurements);

        List<MeasurementJdbcEntity> actual = sut.measurementsSince(APP, TIME_NOW);

        assertEquals(2, actual.size());
        assertThat(actual.get(0)).isEqualToComparingFieldByField(givenMeasurements.get(2));
        assertThat(actual.get(1)).isEqualToComparingFieldByField(givenMeasurements.get(1));
    }

    @Test
    public void givenMeasurements_whenDeletingMeasurementsBefore_thenMeasurementsBeforeAreDeleted() {
        List<MeasurementJdbcEntity> givenMeasurements = Arrays.asList(
                new MeasurementJdbcEntity(TIME_NOW.minusNanos(1000), APP, "asdf", 0),
                new MeasurementJdbcEntity(TIME_NOW, APP, "asdf", -1),
                new MeasurementJdbcEntity(TIME_NOW.plusNanos(1000), APP, "asdf", 0)
        );
        sut.insert(givenMeasurements);

        int actualDeletedCount = sut.deleteAllMeasurementsBefore(TIME_NOW);

        int remainingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM measurements", Integer.class);
        assertEquals(1, actualDeletedCount);
        assertEquals(2, remainingCount);
        List<MeasurementJdbcEntity> remaining = sut.measurementsSince(APP, TIME_NOW.minusHours(1));
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0)).isEqualToComparingFieldByField(givenMeasurements.get(1));
        assertThat(remaining.get(1)).isEqualToComparingFieldByField(givenMeasurements.get(2));
    }

}