package tools.descartes.autoscaling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutoscalerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoscalerApplication.class, args);
    }

}
