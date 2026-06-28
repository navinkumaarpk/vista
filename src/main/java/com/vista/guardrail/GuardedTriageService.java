package com.vista.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.vista.agent.BeamSearchReasoner;
import com.vista.agent.DecideStep;
import com.vista.agent.Decision;
import com.vista.agent.Interpretation;
import com.vista.agent.Observation;
import com.vista.agent.ObserveStep;
import com.vista.guardrail.HumanReviewGate.ReviewOutcome;

@Service
public class GuardedTriageService {

    private static final Logger log = LoggerFactory.getLogger(GuardedTriageService.class);
    private static final double ACCEPT_THRESHOLD = 0.65;

    private final ObserveStep observe;
    private final BeamSearchReasoner reasoner;
    private final DecideStep decide;
    private final HumanReviewGate gate;
    private final MetricsService metrics;
    private final PredictionLog predictionLog;

    public GuardedTriageService(ObserveStep observe, BeamSearchReasoner reasoner,
                                DecideStep decide, HumanReviewGate gate, MetricsService metrics, PredictionLog predictionLog) {
        this.observe = observe;
        this.reasoner = reasoner;
        this.decide = decide;
        this.gate = gate;
        this.metrics = metrics;
        this.predictionLog = predictionLog;
    }

    public record TriageResponse(
            Observation observation, Interpretation interpretation, Decision decision,
            boolean requiresHumanApproval, String approvalReason,
            boolean grounded, long latencyMs, boolean fallback, String predictionId) {}

    public TriageResponse run(String modelKey, String serviceGroup) {
        long start = System.currentTimeMillis();
        Observation obs = observe.observe(serviceGroup);

        Interpretation interp;
        boolean fallback = false;
        try {
            interp = reasoner.reason(modelKey, obs);
        } catch (Exception e) {
            log.error("Reasoning failed; applying safe fallback to escalation", e);
            interp = new Interpretation("UNKNOWN", 0.0, "Reasoning unavailable: " + e.getMessage());
            fallback = true;
        }

        Decision decision = decide.decide(interp);
        boolean grounded = interp.confidence() >= ACCEPT_THRESHOLD;
        ReviewOutcome review = gate.evaluate(decision, grounded);
        long latency = System.currentTimeMillis() - start;

        metrics.record(decision.escalate(), grounded, review.requiresHumanApproval(), fallback, latency);

        String predictionId = predictionLog.record(serviceGroup, interp.classification(), interp.confidence());

        log.info("Guarded result: grounded={} approval={} ({}) latency={}ms fallback={} predictionId={}",
                grounded, review.requiresHumanApproval(), review.reason(), latency, fallback, predictionId);

        return new TriageResponse(obs, interp, decision,
                review.requiresHumanApproval(), review.reason(), grounded, latency, fallback, predictionId);
    }
}