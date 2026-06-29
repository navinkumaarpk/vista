package com.vista.monitor;

import java.time.Instant;

public record MonitorSweep(
        Instant ranAt,
        int groupsScanned,
        int healthy,
        int degraded,
        int findingsRaised,
        long durationMs
) {}