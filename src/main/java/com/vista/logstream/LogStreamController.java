package com.vista.logstream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/logs")
public class LogStreamController {

    private final LogStreamService logStreamService;

    public LogStreamController(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return logStreamService.register();
    }
}