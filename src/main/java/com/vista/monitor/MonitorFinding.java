package com.vista.monitor;

import java.time.Instant;

public record MonitorFinding(
        String serviceGroup,
        Instant detectedAt,
        String severity,        // INFO, WARNING, CRITICAL
        String screeningReason,
        String classification,
        double confidence,
        boolean requiresApproval,
        String recommendedAction,
        String predictionId      // ties this finding to a calibration prediction
) {}