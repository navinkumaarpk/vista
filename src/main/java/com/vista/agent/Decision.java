package com.vista.agent;

public record Decision(
    String recommendedAction,
    boolean escalate,
    boolean highImpact,
    String reason
) {}