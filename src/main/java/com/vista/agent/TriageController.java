package com.vista.agent;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TriageController {

    private final TriageOrchestrator orchestrator;

    public TriageController(TriageOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/triage")
    public TriageResult triage(@RequestBody Map<String, String> body) {
        String serviceGroup = body.getOrDefault("serviceGroup", "");
        String model = body.getOrDefault("model", "claude");
        return orchestrator.run(model, serviceGroup);
    }

    @PostMapping("/triage/reasoner/")
    public TriageResult beamSearchReasoner(@RequestBody Map<String, String> body) {
        String serviceGroup = body.getOrDefault("serviceGroup", "");
        String model = body.getOrDefault("model", "claude");
        return orchestrator.runWithBeamSearchReasoner(model, serviceGroup);
    }
}