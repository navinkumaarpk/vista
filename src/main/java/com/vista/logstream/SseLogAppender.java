package com.vista.logstream;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class SseLogAppender extends AppenderBase<ILoggingEvent> {

    private static volatile LogStreamService sink;

    public static void setSink(LogStreamService s) {
        sink = s;
    }

    @Override
    protected void append(ILoggingEvent event) {
        LogStreamService s = sink;
        if (s == null) return;
        String logger = event.getLoggerName();
        String shortLogger = logger.substring(logger.lastIndexOf('.') + 1);
        String line = event.getLevel() + " " + shortLogger + " — " + event.getFormattedMessage();
        s.broadcast(line);
    }
}