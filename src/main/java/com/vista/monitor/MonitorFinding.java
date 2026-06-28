package com.vista.monitor;

import java.time.Instant;

public record MonitorFinding(
        String serviceGroup,
        Instant detectedAt,
        String screeningReason,
        String classification,
        double confidence,
        boolean requiresApproval,
        String recommendedAction
) {}