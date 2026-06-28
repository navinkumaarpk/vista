package com.vista.guardrail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PredictionLog {

    // In-memory for the build. NOTE: production must persist these (e.g. a Postgres
    // table), because confirmation latency exceeds process lifetime.
    private final Map<String, PredictionRecord> predictions = new ConcurrentHashMap<>();

    public String record(String serviceGroup, String predictedClass, double confidence) {
        String id = UUID.randomUUID().toString();
        predictions.put(id, new PredictionRecord(id, serviceGroup, predictedClass, confidence, Instant.now()));
        return id;
    }

    public boolean resolve(String id, String confirmedClass) {
        PredictionRecord p = predictions.get(id);
        if (p == null) return false;
        p.confirmedClass = confirmedClass;
        p.correct = p.predictedClass.equalsIgnoreCase(confirmedClass);
        p.resolved = true;
        return true;
    }

    public Map<String, Object> calibration() {
        List<PredictionRecord> resolved = predictions.values().stream()
                .filter(p -> p.resolved && p.correct != null).toList();
        long pending = predictions.values().stream().filter(p -> !p.resolved).count();

        double[] edges = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0001};
        List<Map<String, Object>> buckets = new ArrayList<>();
        int n = resolved.size();
        double ece = 0.0;

        for (int i = 0; i < edges.length - 1; i++) {
            final double lo = edges[i], hi = edges[i + 1];
            List<PredictionRecord> inBand = resolved.stream()
                    .filter(p -> p.confidence >= lo && p.confidence < hi).toList();
            int c = inBand.size();
            double acc = c == 0 ? 0 : (double) inBand.stream().filter(p -> p.correct).count() / c;
            double avgConf = c == 0 ? 0 : inBand.stream().mapToDouble(p -> p.confidence).average().orElse(0);
            if (c > 0 && n > 0) ece += ((double) c / n) * Math.abs(avgConf - acc);

            Map<String, Object> b = new LinkedHashMap<>();
            b.put("band", String.format("%.1f-%.1f", lo, Math.min(hi, 1.0)));
            b.put("count", c);
            b.put("accuracy", round(acc));
            b.put("avgConfidence", round(avgConf));
            buckets.add(b);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resolvedPredictions", n);
        out.put("pendingPredictions", pending);
        out.put("expectedCalibrationError", round(ece));
        out.put("buckets", buckets);
        return out;
    }

    private double round(double v) { return Math.round(v * 1000.0) / 1000.0; }
}