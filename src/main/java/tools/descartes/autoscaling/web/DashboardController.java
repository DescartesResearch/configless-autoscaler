package tools.descartes.autoscaling.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tools.descartes.autoscaling.config.RulesProperties;
import tools.descartes.autoscaling.config.ScalingProperties;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final ScalingProperties scalingProperties;
    private final RulesProperties rulesProperties;

    public DashboardController(ScalingProperties scalingProperties, RulesProperties rulesProperties) {
        this.scalingProperties = scalingProperties;
        this.rulesProperties = rulesProperties;
    }

    @GetMapping("/")
    public String showDashboard(Model model) {
        model.addAttribute("config", scalingProperties.showEditableEntries("<br/>"));
        model.addAttribute("constraint", rulesProperties.toStringWithSeparator("<br/>"));
        model.addAttribute("kpis", scalingProperties.showKPIs("<br/>"));
        return "dashboard";
    }

    @GetMapping("/dashboard/config/add")
    public String postConfig(@RequestParam(name="key") String key, @RequestParam(name="value") String value, Model model) {
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
        }
        return showDashboard(model);
    }

    @GetMapping("/dashboard/constraint/add")
    public String postConstraint(@RequestParam(name="app") String app,
                                 @RequestParam(name="relation") String relation,
                                 @RequestParam(name="value") int value,
                                 Model model) {
        RulesProperties.ScalingExpression se = new RulesProperties.ScalingExpression();
        se.setApp(app);
        se.setRelation(Enum.valueOf(RulesProperties.ScalingExpression.Relation.class, relation));
        se.setValue(value);
        rulesProperties.getRules().add(se);
        return showDashboard(model);
    }

    @GetMapping("/dashboard/constraint/delete")
    public String deleteConstraint(@RequestParam(name="app") String app, Model model) {
        List<RulesProperties.ScalingExpression> toDelete = rulesProperties.getRules().stream()
                .filter(se -> se.getApp().equals(app))
                .collect(Collectors.toList());
        rulesProperties.getRules().removeAll(toDelete);
        return showDashboard(model);
    }
}
