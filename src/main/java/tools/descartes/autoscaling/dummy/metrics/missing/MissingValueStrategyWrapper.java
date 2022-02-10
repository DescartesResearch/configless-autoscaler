package tools.descartes.autoscaling.dummy.metrics.missing;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Wrapps and instantiates a concrete implementation of MissingValueStrategy
 */
@Component
@Getter
public class MissingValueStrategyWrapper {
    private final MissingValueStrategy defaultStrategy;

    public MissingValueStrategyWrapper() {
        defaultStrategy = new ConstantStrategy(-1);
    }

}
