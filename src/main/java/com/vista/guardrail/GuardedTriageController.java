package com.vista.guardrail;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GuardedTriageController {

    private final GuardedTriageService guarded;
    private final MetricsService metrics;
    private final PredictionLog predictionLog;

    public GuardedTriageController(GuardedTriageService guarded, MetricsService metrics, PredictionLog predictionLog) {
        this.guarded = guarded;
        this.metrics = metrics;
        this.predictionLog = predictionLog;
    }

    @PostMapping("/triage/guarded")
    public GuardedTriageService.TriageResponse triage(@RequestBody Map<String, String> body) {
        return guarded.run(body.getOrDefault("model", "claude"),
                           body.getOrDefault("serviceGroup", ""));
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return metrics.snapshot();
    }

    @PostMapping("/predictions/{id}/outcome")
    public Map<String, Object> resolve(@PathVariable String id, @RequestBody Map<String, String> body) {
    boolean ok = predictionLog.resolve(id, body.getOrDefault("confirmedClassification", ""));
    return Map.of("resolved", ok, "predictionId", id);
    }

    @GetMapping("/metrics/calibration")
    public Map<String, Object> calibration() {
        return predictionLog.calibration();
    }
}