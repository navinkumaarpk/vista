package com.vista.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TriageOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TriageOrchestrator.class);
    private final ObserveStep observe;
    private final InterpretStep interpret;
    private final BeamSearchReasoner reasoner;
    private final DecideStep decide;

    public TriageOrchestrator(ObserveStep observe, BeamSearchReasoner reasoner, InterpretStep interpret, DecideStep decide) {
        this.observe = observe;
        this.interpret = interpret;
        this.decide = decide;
        this.reasoner = reasoner;
    }

    public TriageResult run(String modelKey, String serviceGroup) {
        log.info("=== Triage start: sg={} model={} ===", serviceGroup, modelKey);
        Observation observation = observe.observe(serviceGroup);
        log.info("Observed: {}", observation.summary().replaceAll("\\s+", " ").trim());
        Interpretation interpretation = interpret.interpret(modelKey, observation);
        Decision decision = decide.decide(interpretation);
        log.info("Decision: action='{}' escalate={}", decision.recommendedAction(), decision.escalate());
        return new TriageResult(observation, interpretation, decision);
    }

    public TriageResult runWithBeamSearchReasoner(String modelKey, String serviceGroup) {
        log.info("=== Triage start: sg={} model={} ===", serviceGroup, modelKey);
        Observation observation = observe.observe(serviceGroup);
        log.info("Observed: {}", observation.summary().replaceAll("\\s+", " ").trim());
        Interpretation interpretation = reasoner.reason(modelKey, observation);   // BeamSearch Reasoning
        Decision decision = decide.decide(interpretation);
        log.info("Decision: action='{}' escalate={}", decision.recommendedAction(), decision.escalate());
        return new TriageResult(observation, interpretation, decision);
    }
}