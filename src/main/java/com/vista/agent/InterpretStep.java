package com.vista.agent;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.vista.chat.ChatService;
import com.vista.prompt.PromptLibrary;
import com.vista.rag.KnowledgeBaseService;

@Service
public class InterpretStep {

    private static final Logger log = LoggerFactory.getLogger(InterpretStep.class);
    private static final int TOP_K = 3;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final ChatService chat;
    private final KnowledgeBaseService kb;
    private final PromptLibrary prompts;

    public InterpretStep(ChatService chat, KnowledgeBaseService kb, PromptLibrary prompts) {
        this.chat = chat;
        this.kb = kb;
        this.prompts = prompts;
    }

    public Interpretation interpret(String modelKey, Observation obs) {
        List<Document> precedent = kb.findSimilar(obs.summary(), TOP_K, SIMILARITY_THRESHOLD);

        String precedentText = precedent.isEmpty()
                ? "No similar historical cases found in the knowledge base."
                : precedent.stream()
                    .map(d -> "- Past symptom: " + d.getText()
                            + " | Classified as: " + d.getMetadata().get("classification")
                            + " | Resolved by: " + d.getMetadata().get("resolution"))
                    .reduce("", (a, b) -> a + "\n" + b);

        String groundingNote = precedent.isEmpty()
                ? "Since no precedent was found, mark confidence no higher than 0.4 and lean toward UNKNOWN unless the live data is unambiguous."
                : "Use the precedent below to support your classification.";

        String prompt = prompts.render("interpret", Map.of(
                "grounding", groundingNote,
                "precedent", precedentText,
                "observation", obs.summary()));

        Interpretation result = chat.structured(modelKey, prompt, Interpretation.class);
        log.info("Interpretation: class={} confidence={} grounded={}",
                result.classification(), result.confidence(), !precedent.isEmpty());
        return result;
    }
}