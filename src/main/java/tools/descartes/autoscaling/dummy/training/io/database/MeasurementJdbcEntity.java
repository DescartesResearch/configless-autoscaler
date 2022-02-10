package tools.descartes.autoscaling.dummy.training.io.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeasurementJdbcEntity {
    private LocalDateTime measurementTime;
    private String app;
    private String measurementJson;
    private Integer actions;
}
