package com.vista.agent;

public record TriageResult(
    Observation observation,
    Interpretation interpretation,
    Decision decision
) {}
