package com.vista.agent;

public record ScoredBranch(
        String category,
        double signalAlignment,
        double retrievalSupport,
        double scopeConsistency,
        double resolutionFeasibility,
        double composite,
        String rationale
) {}