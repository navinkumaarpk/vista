package com.vista.logstream;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;

@Configuration
public class LogStreamConfig {

    private final LogStreamService logStreamService;

    public LogStreamConfig(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    @PostConstruct
    public void attach() {
        SseLogAppender.setSink(logStreamService);

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        SseLogAppender appender = new SseLogAppender();
        appender.setContext(ctx);
        appender.start();

        // High-signal agent steps:
        ctx.getLogger("com.vista").addAppender(appender);
        // The model's prompts and responses (what it "thinks"):
        ctx.getLogger("org.springframework.ai.chat.client.advisor").addAppender(appender);
    }
}