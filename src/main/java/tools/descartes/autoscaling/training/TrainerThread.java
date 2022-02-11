package tools.descartes.autoscaling.training;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.config.ScalingProperties;

/**
 * A wrapper for a thread which runs the logic in the Trainer class
 */
@Component
public class TrainerThread {

    private static final Logger logger = LoggerFactory.getLogger(TrainerThread.class);

    private final Trainer trainer;
    private final ScalingProperties scalingProperties;

    public TrainerThread(Trainer trainer, ScalingProperties scalingProperties) {
        this.trainer = trainer;
        this.scalingProperties = scalingProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autostart() {
        if (scalingProperties.isAutostart()) {
            logger.info("Autostarting model trainer.");
            start();
        } else {
            logger.info("Model trainer is idle. Trigger Rest-Controller to start autoscaler.");
        }
    }

    public void start() {
        new Thread(trainer).start();
    }

    public void stop() {
        trainer.exit();
    }
}
