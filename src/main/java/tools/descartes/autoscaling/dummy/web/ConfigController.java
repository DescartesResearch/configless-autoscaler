package tools.descartes.autoscaling.dummy.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.descartes.autoscaling.dummy.config.ScalingProperties;

@RestController
public class ConfigController {

    private final ScalingProperties scalingProperties;

    public ConfigController(ScalingProperties scalingProperties) {
        this.scalingProperties = scalingProperties;
    }

    @GetMapping("/config/show")
    public String getConfig() {
        return scalingProperties.showEditableEntries("\n");
    }

    @GetMapping("/config/add")
    public String postConfig(@RequestParam(name="key") String key, @RequestParam(name="value") String value) {
        switch (key) {
            case "scaling.upscale.limit":
                scalingProperties.getUpscale().setLimit(Integer.parseInt(value));
                break;
            case "scaling.upscale.timeout":
                scalingProperties.getUpscale().setTimeout(Integer.parseInt(value));
                break;
            case "scaling.downscale.limit":
                scalingProperties.getDownscale().setLimit(Integer.parseInt(value));
                break;
            case "scaling.downscale.timeout":
                scalingProperties.getDownscale().setTimeout(Integer.parseInt(value));
                break;
            default:
                return "No valid key.";
        }
        return getConfig();
    }
}
