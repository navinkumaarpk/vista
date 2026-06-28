package com.vista.agent;

public record Interpretation(
    String classification,   // PLANT, CPE, PROVISIONING, HEALTHY, UNKNOWN
    double confidence,       // 0.0 - 1.0
    String rationale
) {}
