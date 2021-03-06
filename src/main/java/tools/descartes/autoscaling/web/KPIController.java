package tools.descartes.autoscaling.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.descartes.autoscaling.config.ScalingProperties;

@RestController
public class KPIController {
    private final ScalingProperties scalingProperties;

    public KPIController(ScalingProperties scalingProperties) {
        this.scalingProperties = scalingProperties;
    }

    @GetMapping("/kpis/show")
    public String getKpis() {
        return scalingProperties.showKPIs("\n");
    }
}
