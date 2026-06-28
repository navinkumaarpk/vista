package com.vista.agent;

import org.springframework.stereotype.Service;

@Service
public class DecideStep {

    private static final double CONFIDENCE_THRESHOLD = 0.65;  // was 0.5

    public Decision decide(Interpretation interp) {
        if (interp.confidence() < CONFIDENCE_THRESHOLD) {
            return new Decision(
                    "Escalate to a senior engineer for manual review",
                    true, false,
                    "Confidence " + interp.confidence() + " is below threshold " + CONFIDENCE_THRESHOLD);
        }

        return switch (interp.classification().toUpperCase()) {
            case "PLANT" -> new Decision(
                    "Dispatch field technician to inspect upstream plant for ingress noise",
                    false, true, "High-confidence plant-level classification");
            case "CPE" -> new Decision(
                    "Schedule CPE diagnostics; possible modem replacement",
                    false, true, "High-confidence CPE classification");
            case "PROVISIONING" -> new Decision(
                    "Review and correct provisioning state for affected modems",
                    false, false, "High-confidence provisioning classification");
            case "HEALTHY" -> new Decision(
                    "No action required; continue monitoring",
                    false, false, "Service group classified healthy");
            default -> new Decision(
                    "Escalate for manual review",
                    true, false, "Unrecognized or uncertain classification");
        };
    }
}
