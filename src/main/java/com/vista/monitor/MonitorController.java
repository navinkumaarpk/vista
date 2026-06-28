package com.vista.monitor;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorAgent monitor;

    public MonitorController(MonitorAgent monitor) {
        this.monitor = monitor;
    }

    @PostMapping("/start")
    public Map<String, Object> start() { monitor.start(); return status(); }

    @PostMapping("/stop")
    public Map<String, Object> stop() { monitor.stop(); return status(); }

    @PostMapping("/run")
    public Map<String, Object> runNow() {
        monitor.pollOnce();    // manual trigger, one cycle now
        return Map.of("status", "poll completed", "findings", monitor.getFindings());
    }

    @PostMapping("/interval")
    public Map<String, Object> setInterval(@RequestBody Map<String, Long> body) {
        monitor.setIntervalSeconds(body.getOrDefault("seconds", 60L));
        return status();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "enabled", monitor.isEnabled(),
                "intervalSeconds", monitor.getIntervalSeconds(),
                "model", monitor.getModel(),
                "lastRun", String.valueOf(monitor.getLastRun()),
                "findingsCount", monitor.getFindings().size());
    }

    @GetMapping("/findings")
    public List<MonitorFinding> findings() { return monitor.getFindings(); }
}