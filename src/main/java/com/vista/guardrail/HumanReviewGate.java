package com.vista.guardrail;

import org.springframework.stereotype.Component;

import com.vista.agent.Decision;

@Component
public class HumanReviewGate {

    public record ReviewOutcome(boolean requiresHumanApproval, String reason) {}

    public ReviewOutcome evaluate(Decision decision, boolean grounded) {
        if (decision.escalate())
            return new ReviewOutcome(true, "Confidence below threshold; routed to human review.");
        if (!grounded)
            return new ReviewOutcome(true, "Ungrounded recommendation (no supporting precedent); requires verification.");
        if (decision.highImpact())
            return new ReviewOutcome(true, "High-impact action requires engineer approval before execution.");
        return new ReviewOutcome(false, "Autonomous action permitted (grounded, high-confidence, low-impact).");
    }
}