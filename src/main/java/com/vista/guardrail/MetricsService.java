package com.vista.guardrail;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong escalations = new AtomicLong();
    private final AtomicLong grounded = new AtomicLong();
    private final AtomicLong approvals = new AtomicLong();
    private final AtomicLong fallbacks = new AtomicLong();
    private final AtomicLong latencySum = new AtomicLong();

    public void record(boolean escalated, boolean wasGrounded, boolean approvalRequired,
                       boolean fallback, long latencyMs) {
        total.incrementAndGet();
        if (escalated) escalations.incrementAndGet();
        if (wasGrounded) grounded.incrementAndGet();
        if (approvalRequired) approvals.incrementAndGet();
        if (fallback) fallbacks.incrementAndGet();
        latencySum.addAndGet(latencyMs);
    }

    public Map<String, Object> snapshot() {
        long t = total.get();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalTriages", t);
        m.put("escalationRate", rate(escalations.get(), t));
        m.put("groundednessRate", rate(grounded.get(), t));
        m.put("humanApprovalRate", rate(approvals.get(), t));
        m.put("fallbackRate", rate(fallbacks.get(), t));
        m.put("avgLatencyMs", t == 0 ? 0 : latencySum.get() / t);
        return m;
    }

    private double rate(long n, long d) {
        return d == 0 ? 0.0 : Math.round((double) n / d * 1000.0) / 1000.0;
    }
}