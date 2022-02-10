package tools.descartes.autoscaling.dummy.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.descartes.autoscaling.dummy.config.RulesProperties;

import java.util.List;
import java.util.stream.Collectors;

import static tools.descartes.autoscaling.dummy.config.RulesProperties.ScalingExpression;

@RestController
public class ConstraintController {

    private final RulesProperties rulesProperties;

    public ConstraintController(RulesProperties rulesProperties) {
        this.rulesProperties = rulesProperties;
    }

    @GetMapping("/constraint/show")
    public String getConstraint() {
        return rulesProperties.toString();
    }

    @GetMapping("/constraint/add")
    public String postConstraint(@RequestParam(name="app") String app,
                               @RequestParam(name="relation") String relation,
                               @RequestParam(name="value") int value) {
        ScalingExpression se = new ScalingExpression();
        se.setApp(app);
        se.setRelation(Enum.valueOf(ScalingExpression.Relation.class, relation));
        se.setValue(value);
        rulesProperties.getRules().add(se);
        return getConstraint();
    }

    @GetMapping("/constraint/delete")
    public String deleteConstraint(@RequestParam(name="app") String app) {
        List<ScalingExpression> toDelete = rulesProperties.getRules().stream()
                .filter(se -> se.getApp().equals(app))
                .collect(Collectors.toList());
        rulesProperties.getRules().removeAll(toDelete);
        return getConstraint();
    }
}
