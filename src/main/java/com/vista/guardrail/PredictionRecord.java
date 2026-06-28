package com.vista.guardrail;

import java.time.Instant;

public class PredictionRecord {
    public final String id;
    public final String serviceGroup;
    public final String predictedClass;
    public final double confidence;
    public final Instant predictedAt;
    public volatile boolean resolved = false;
    public volatile Boolean correct = null;       // null until ground truth arrives
    public volatile String confirmedClass = null;

    public PredictionRecord(String id, String sg, String cls, double conf, Instant at) {
        this.id = id; this.serviceGroup = sg; this.predictedClass = cls;
        this.confidence = conf; this.predictedAt = at;
    }
}