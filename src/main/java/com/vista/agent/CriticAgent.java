package com.vista.agent;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import com.vista.chat.ChatService;
import com.vista.prompt.PromptLibrary;

@Component
public class CriticAgent {

    // composite weights (sum to 1.0)
    private static final double W_SIGNAL = 0.35;
    private static final double W_RETRIEVAL = 0.30;
    private static final double W_SCOPE = 0.20;
    private static final double W_RESOLUTION = 0.15;

    private final ChatService chat;
    private final PromptLibrary prompts;

    public CriticAgent(ChatService chat, PromptLibrary prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    public ScoredBranch score(String modelKey, Hypothesis h, Observation obs, List<Document> precedent) {
        // Deterministic retrieval support: how much retrieved precedent matches THIS category.
        long matching = precedent.stream()
                .filter(d -> h.category().equalsIgnoreCase(
                        String.valueOf(d.getMetadata().get("classification"))))
                .count();
        double retrievalSupport = matching > 0 ? Math.min(1.0, 0.6 + 0.1 * matching) : 0.25;

        String precedentText = precedent.isEmpty()
                ? "No historical precedent available."
                : precedent.stream()
                    .map(d -> "- " + d.getText() + " (was: " + d.getMetadata().get("classification") + ")")
                    .reduce("", (a, b) -> a + "\n" + b);

        
        String prompt = prompts.render("critic", Map.of(
                            "category", h.category(),
                            "precedent", precedentText,
                            "observation", obs.summary()));                          

        CriticAssessment a = chat.structured(modelKey, prompt, CriticAssessment.class);

        double composite = W_SIGNAL * a.signalAlignment()
                + W_RETRIEVAL * retrievalSupport
                + W_SCOPE * a.scopeConsistency()
                + W_RESOLUTION * a.resolutionFeasibility();

        return new ScoredBranch(h.category(),
                a.signalAlignment(), retrievalSupport, a.scopeConsistency(),
                a.resolutionFeasibility(), composite, a.rationale());
    }
}