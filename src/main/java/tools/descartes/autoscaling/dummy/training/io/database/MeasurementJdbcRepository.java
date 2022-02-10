package tools.descartes.autoscaling.dummy.training.io.database;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MeasurementJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /*
     * Postgres-specific, does not run on h2
     */
    public int insert(List<MeasurementJdbcEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return 0;
        }
        return Arrays.stream(jdbcTemplate.batchUpdate(
                "INSERT INTO measurements (measurement_time,app,measurement_json,actions) VALUES (?, ? ,?, ?)",
                entities.stream()
                        .map(e -> new Object[]{
                                e.getMeasurementTime(), e.getApp(), e.getMeasurementJson(), e.getActions()
                        })
                        .collect(Collectors.toList())
        )).sum();
    }

    public List<MeasurementJdbcEntity> measurementsSince(String app, LocalDateTime since) {
        return jdbcTemplate.query(
                "SELECT measurement_time,app,measurement_json,actions FROM measurements " +
                        "WHERE app = ? AND measurement_time >= ? ORDER BY measurement_time ASC",
                new BeanPropertyRowMapper<>(MeasurementJdbcEntity.class), app, since
        );
    }

    public int deleteAllMeasurementsBefore(LocalDateTime beforeTime) {
        return jdbcTemplate.update("DELETE from measurements WHERE measurement_time < ?", beforeTime);
    }
}
