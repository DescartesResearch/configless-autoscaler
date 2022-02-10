package tools.descartes.autoscaling.dummy.scaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.descartes.autoscaling.dummy.config.ScalingProperties;

/**
 * A wrapper for a thread which runs the logic in the Scaler class
 */
@Component
public class ScalerThread {

    private static final Logger logger = LoggerFactory.getLogger(ScalerThread.class);

    private final Scaler scaler;
    private final ScalingProperties scalingProperties;

    public ScalerThread(Scaler scaler, ScalingProperties scalingProperties) {
        this.scaler = scaler;
        this.scalingProperties = scalingProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void autostart() {
        if (scalingProperties.isAutostart()) {
            logger.info("Autostarting autoscaler.");
            start();
        } else {
            logger.info("Autoscaler is idle. Trigger Rest-Controller to start.");
        }
    }

    public void start() {
        new Thread(scaler).start();
    }

    public void stop() {
        scaler.exit();
    }
}
