package tools.descartes.autoscaling.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.descartes.autoscaling.scaling.ScalerThread;
import tools.descartes.autoscaling.training.TrainerThread;

@RestController
public class DummyAutoscalerController {

    private final ScalerThread scalerThread;
    private final TrainerThread trainerThread;

    public DummyAutoscalerController(ScalerThread scalerThread, TrainerThread trainerThread) {
        this.scalerThread = scalerThread;
        this.trainerThread = trainerThread;
    }

    @GetMapping("/start")
    public String start() {
        scalerThread.start();
        trainerThread.start();
        return "Start successful";
    }

    @GetMapping("/stop")
    public String stop() {
        scalerThread.stop();
        trainerThread.stop();
        return "Stopped";
    }

    @GetMapping("/startScaling")
    public String startScaling() {
        scalerThread.start();
        return "Start successful";
    }

    @GetMapping("/stopScaling")
    public String stopScaling() {
        scalerThread.stop();
        return "Stopped";
    }

    @GetMapping("/startTraining")
    public String startTraining() {
        trainerThread.start();
        return "Start successful";
    }

    @GetMapping("/stopTraining")
    public String stopTraining() {
        trainerThread.stop();
        return "Stopped";
    }
}
