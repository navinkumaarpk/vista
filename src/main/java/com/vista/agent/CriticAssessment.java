package com.vista.agent;

public record CriticAssessment(
        double signalAlignment,        // 0-1: how well telemetry matches this fault pattern
        double scopeConsistency,       // 0-1: isolated vs distributed pattern fits this cause
        double resolutionFeasibility,  // 0-1: is the implied fix actionable
        String rationale
) {}